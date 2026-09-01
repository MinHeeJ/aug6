package kr.ac.knue.commonfoundation.exceptionperiod;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExceptionPeriodService {
    private static final Set<String> ADMIN_ROLES = Set.of("R04", "R09");
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Set<String> FINAL_STATUSES = Set.of("평가확정", "EVALUATION_CONFIRMED", "FINAL_CONFIRMED");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private final ExceptionPeriodMapper mapper;

    public ExceptionPeriodService(ExceptionPeriodMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ExceptionPeriodSearchResponse list(CurrentUser user, ExceptionPeriodSearchCriteria criteria) {
        requireExceptionPeriodRole(user);
        return new ExceptionPeriodSearchResponse(mapper.listExceptionPeriods(criteria), Math.max(criteria.page(), 0),
                criteria.safeSize(), mapper.countExceptionPeriods(criteria));
    }

    @Transactional
    public ExceptionPeriodRow save(SaveExceptionPeriodRequest request, CurrentUser user, String requestId) {
        requireExceptionPeriodRole(user);
        SaveExceptionPeriodRequest normalized = normalizeAndValidate(request);
        if (mapper.existsTeacherUser(normalized.teacherUserId()) == 0) {
            throw new BusinessValidationException("예외기간 대상 교원이 올바르지 않습니다.",
                    List.of(new ValidationError("teacherUserId", "기존 교원 사용자를 선택하세요.")));
        }
        if (mapper.existsEvaluationArea(normalized.areaCode()) == 0) {
            throw new BusinessValidationException("예외기간 평가영역이 올바르지 않습니다.",
                    List.of(new ValidationError("areaCode", "기존 평가영역 코드를 선택하세요.")));
        }
        ExceptionPeriodRow before = null;
        if (normalized.settingId() != null) {
            before = mapper.findExceptionPeriodById(normalized.settingId());
            if (before == null) throw new NotFoundException("예외기간 설정을 찾을 수 없습니다.");
        }
        if ("Y".equals(normalized.activeYn()) && mapper.countOverlappingExceptionPeriods(normalized.settingId(),
                normalized.evaluationYear(), normalized.teacherUserId(), normalized.areaCode(), normalized.targetFunctionCode(),
                normalized.exceptionStartAt(), normalized.exceptionEndAt()) > 0) {
            throw new ConflictException("동일 평가연도·교원·영역·기능 기준의 활성 예외기간이 중복됩니다.");
        }
        ExceptionPeriodRow after = normalized.settingId() == null
                ? mapper.insertExceptionPeriod(normalized, user.userId())
                : mapper.updateExceptionPeriod(normalized, user.userId());
        recordChangeHistory(before, after, user.userId(), normalized.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public ExceptionPeriodDecision evaluateModificationAccess(String evaluationYear, Long teacherUserId, String areaCode,
                                                              String targetFunctionCode, String organizationCode,
                                                              LocalDateTime requestAt, String achievementStatus) {
        List<ValidationError> fields = new ArrayList<>();
        String normalizedEvaluationYear = trim(evaluationYear);
        String normalizedAreaCode = normalized(areaCode);
        String normalizedFunctionCode = normalized(targetFunctionCode);
        String normalizedOrganizationCode = normalized(organizationCode);
        if (!hasText(normalizedEvaluationYear)) fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        if (teacherUserId == null) fields.add(new ValidationError("teacherUserId", "대상 교원을 입력하세요."));
        if (!hasText(normalizedAreaCode)) fields.add(new ValidationError("areaCode", "평가영역을 입력하세요."));
        if (!hasText(normalizedFunctionCode)) fields.add(new ValidationError("targetFunctionCode", "대상 기능을 입력하세요."));
        if (requestAt == null) fields.add(new ValidationError("requestAt", "요청 일시를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("예외기간 수정 판정 요청이 올바르지 않습니다.", fields);
        if (FINAL_STATUSES.contains(trim(achievementStatus))) {
            return ExceptionPeriodDecision.deny("평가확정 상태 데이터는 예외기간 안에서도 수정·삭제할 수 없습니다.");
        }
        ExceptionPeriodRow exception = mapper.findActiveExceptionPeriodForModification(teacherUserId, normalizedAreaCode,
                normalizedFunctionCode, requestAt);
        if (exception != null) return ExceptionPeriodDecision.allow(exception);
        if (mapper.countActiveModificationPeriods(normalizedEvaluationYear, normalizedAreaCode, normalizedOrganizationCode, requestAt) > 0) {
            return ExceptionPeriodDecision.allowByGeneralPeriod();
        }
        return ExceptionPeriodDecision.deny("예외기간이 종료되어 일반 수정기간 규칙으로 복귀했습니다.");
    }

    CurrentUser requireExceptionPeriodRole(CurrentUser user) {
        if (user == null) throw new UnauthenticatedException();
        if (user.roles().stream().anyMatch(ADMIN_ROLES::contains)) return user;
        throw new ForbiddenException();
    }

    private SaveExceptionPeriodRequest normalizeAndValidate(SaveExceptionPeriodRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String evaluationYear = trim(request.evaluationYear());
        String areaCode = normalized(request.areaCode());
        String targetFunctionCode = normalized(request.targetFunctionCode());
        String activeYn = normalized(request.activeYn());
        String approvalReason = trim(request.approvalReason());
        String changeReason = trim(request.changeReason());
        if (!hasText(evaluationYear)) fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        else if (!EVALUATION_YEAR.matcher(evaluationYear).matches()) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (request.teacherUserId() == null) fields.add(new ValidationError("teacherUserId", "대상 교원을 입력하세요."));
        if (!hasText(areaCode)) fields.add(new ValidationError("areaCode", "평가영역 코드를 입력하세요."));
        if (!hasText(targetFunctionCode)) fields.add(new ValidationError("targetFunctionCode", "대상 기능 코드를 입력하세요."));
        if (request.exceptionStartAt() == null) fields.add(new ValidationError("exceptionStartAt", "예외 시작일시를 입력하세요."));
        if (request.exceptionEndAt() == null) fields.add(new ValidationError("exceptionEndAt", "예외 종료일시를 입력하세요."));
        if (request.exceptionStartAt() != null && request.exceptionEndAt() != null && request.exceptionEndAt().isBefore(request.exceptionStartAt())) {
            fields.add(new ValidationError("exceptionEndAt", "종료일시는 시작일시 이후여야 합니다."));
        }
        if (!hasText(approvalReason)) fields.add(new ValidationError("approvalReason", "승인사유를 입력하세요."));
        if (activeYn == null || !USE_FLAGS.contains(activeYn)) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("예외기간 저장 요청이 올바르지 않습니다.", fields);
        return new SaveExceptionPeriodRequest(request.settingId(), evaluationYear, request.teacherUserId(), areaCode,
                targetFunctionCode, request.exceptionStartAt(), request.exceptionEndAt(), approvalReason, activeYn, changeReason);
    }

    private void recordChangeHistory(ExceptionPeriodRow before, ExceptionPeriodRow after, Long userId, String changeReason, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory("exception_period_settings", String.valueOf(after.settingId()), changeType,
                    "setting", beforeValue, afterValue, userId, changeReason, requestId);
        }
    }

    private String summary(ExceptionPeriodRow row) {
        return row.evaluationYear() + ":" + row.teacherUserId() + ":" + row.areaCode() + ":" + row.targetFunctionCode();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
