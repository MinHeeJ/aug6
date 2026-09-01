package kr.ac.knue.commonfoundation.appealperiod;

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
public class AppealPeriodService {
    private static final Set<String> ADMIN_ROLES = Set.of("R04", "R09");
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private final AppealPeriodMapper mapper;

    public AppealPeriodService(AppealPeriodMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AppealPeriodSearchResponse list(CurrentUser user, AppealPeriodSearchCriteria criteria) {
        requireAppealAdminRole(user);
        AppealPeriodSearchCriteria scoped = new AppealPeriodSearchCriteria(
                criteria.page(), criteria.pageSize(), criteria.evaluationYear(), criteria.collegeOrganizationCode(),
                criteria.departmentOrganizationCode(), criteria.activeYn(), criteria.keyword(), user.userId(),
                shouldRestrictOrganizationScope(user));
        return new AppealPeriodSearchResponse(mapper.listAppealPeriods(scoped), Math.max(criteria.page(), 0),
                scoped.safeSize(), mapper.countAppealPeriods(scoped));
    }

    @Transactional
    public AppealPeriodRow save(SaveAppealPeriodRequest request, CurrentUser user, String requestId) {
        requireAppealAdminRole(user);
        SaveAppealPeriodRequest normalized = normalizeAndValidate(request);
        requireOrganizationScope(user, normalized.collegeOrganizationCode(), normalized.departmentOrganizationCode());
        if (mapper.existsHandlerUserForAppealPeriod(normalized.handlerUserId(), normalized.collegeOrganizationCode(),
                normalized.departmentOrganizationCode()) == 0) {
            throw new BusinessValidationException("이의신청 처리 담당자가 소속 또는 처리 역할 범위에 없습니다.",
                    List.of(new ValidationError("handlerUserId", "기존 사용자와 담당 소속/역할 범위를 확인하세요.")));
        }
        AppealPeriodRow before = null;
        if (normalized.settingId() != null) {
            before = mapper.findAppealPeriodById(normalized.settingId());
            if (before == null) {
                throw new NotFoundException("이의신청기간 설정을 찾을 수 없습니다.");
            }
        }
        if ("Y".equals(normalized.activeYn()) && mapper.countOverlappingAppealPeriods(normalized.settingId(),
                normalized.evaluationYear(), normalized.collegeOrganizationCode(), normalized.departmentOrganizationCode(),
                normalized.appealStartAt(), normalized.appealEndAt()) > 0) {
            throw new ConflictException("동일 평가연도·소속 기준의 활성 이의신청기간이 중복됩니다.");
        }
        AppealPeriodRow after = normalized.settingId() == null
                ? mapper.insertAppealPeriod(normalized, user.userId())
                : mapper.updateAppealPeriod(normalized, user.userId());
        recordChangeHistory(before, after, user.userId(), normalized.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public AppealPeriodDecision evaluateSubmission(String applicantOrganizationCode, LocalDateTime requestAt) {
        List<ValidationError> fields = new ArrayList<>();
        String organizationCode = normalized(applicantOrganizationCode);
        if (!hasText(organizationCode)) fields.add(new ValidationError("applicantOrganizationCode", "신청자 소속 코드를 입력하세요."));
        if (requestAt == null) fields.add(new ValidationError("requestAt", "요청 일시를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("이의신청 제출 기간 판정 요청이 올바르지 않습니다.", fields);
        }
        AppealPeriodRow row = mapper.findActiveAppealPeriodForSubmission(organizationCode, requestAt);
        if (row == null) {
            return AppealPeriodDecision.deny("이의신청 제출 가능 기간이 아닙니다.");
        }
        return AppealPeriodDecision.allow(row);
    }

    @Transactional(readOnly = true)
    public AppealPeriodDecision evaluateHandler(Long handlerUserId, Long appealPeriodSettingId, LocalDateTime requestAt) {
        List<ValidationError> fields = new ArrayList<>();
        if (handlerUserId == null) fields.add(new ValidationError("handlerUserId", "담당자 사용자 ID를 입력하세요."));
        if (appealPeriodSettingId == null) fields.add(new ValidationError("appealPeriodSettingId", "이의신청기간 설정 ID를 입력하세요."));
        if (requestAt == null) fields.add(new ValidationError("requestAt", "요청 일시를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("이의신청 담당자 기간 판정 요청이 올바르지 않습니다.", fields);
        }
        AppealPeriodRow row = mapper.findAppealPeriodById(appealPeriodSettingId);
        if (row == null || !"Y".equals(row.activeYn()) || !Objects.equals(handlerUserId, row.handlerUserId())
                || requestAt.isBefore(row.appealStartAt()) || requestAt.isAfter(row.appealEndAt())) {
            return AppealPeriodDecision.deny("이의신청 담당자 처리 가능 범위가 아닙니다.");
        }
        return AppealPeriodDecision.allow(row);
    }

    CurrentUser requireAppealAdminRole(CurrentUser user) {
        if (user == null) throw new UnauthenticatedException();
        if (user.roles().stream().anyMatch(ADMIN_ROLES::contains)) return user;
        throw new ForbiddenException();
    }

    private boolean shouldRestrictOrganizationScope(CurrentUser user) {
        return !user.roles().contains("R09") && user.roles().contains("R04");
    }

    private void requireOrganizationScope(CurrentUser user, String collegeOrganizationCode, String departmentOrganizationCode) {
        if (!shouldRestrictOrganizationScope(user)) return;
        boolean allowed = mapper.existsAuthorizedEvaluationOrganization(user.userId(), collegeOrganizationCode) > 0
                || mapper.existsAuthorizedEvaluationOrganization(user.userId(), departmentOrganizationCode) > 0;
        if (!allowed) throw new ForbiddenException();
    }

    private SaveAppealPeriodRequest normalizeAndValidate(SaveAppealPeriodRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String evaluationYear = trim(request.evaluationYear());
        String collegeOrganizationCode = normalized(request.collegeOrganizationCode());
        String departmentOrganizationCode = normalized(request.departmentOrganizationCode());
        String activeYn = normalized(request.activeYn());
        String changeReason = trim(request.changeReason());
        if (!hasText(evaluationYear)) fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        else if (!EVALUATION_YEAR.matcher(evaluationYear).matches()) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!hasText(collegeOrganizationCode)) fields.add(new ValidationError("collegeOrganizationCode", "소속대학 코드를 입력하세요."));
        if (request.appealStartAt() == null) fields.add(new ValidationError("appealStartAt", "이의신청 시작일시를 입력하세요."));
        if (request.appealEndAt() == null) fields.add(new ValidationError("appealEndAt", "이의신청 종료일시를 입력하세요."));
        if (request.appealStartAt() != null && request.appealEndAt() != null && request.appealEndAt().isBefore(request.appealStartAt())) {
            fields.add(new ValidationError("appealEndAt", "종료일시는 시작일시 이후여야 합니다."));
        }
        if (request.handlerUserId() == null) fields.add(new ValidationError("handlerUserId", "처리 담당자를 입력하세요."));
        if (activeYn == null || !USE_FLAGS.contains(activeYn)) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("이의신청기간 저장 요청이 올바르지 않습니다.", fields);
        return new SaveAppealPeriodRequest(request.settingId(), evaluationYear, collegeOrganizationCode,
                departmentOrganizationCode, request.appealStartAt(), request.appealEndAt(), request.handlerUserId(),
                activeYn, changeReason);
    }

    private void recordChangeHistory(AppealPeriodRow before, AppealPeriodRow after, Long userId, String changeReason, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory("appeal_period_settings", String.valueOf(after.settingId()), changeType,
                    "setting", beforeValue, afterValue, userId, changeReason, requestId);
        }
    }

    private String summary(AppealPeriodRow row) {
        return row.evaluationYear() + ":" + row.collegeOrganizationCode() + ":" + row.departmentOrganizationCode()
                + ":" + row.handlerUserId();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
