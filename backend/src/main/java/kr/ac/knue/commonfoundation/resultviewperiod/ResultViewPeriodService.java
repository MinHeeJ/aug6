package kr.ac.knue.commonfoundation.resultviewperiod;

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
public class ResultViewPeriodService {
    private static final Set<String> ADMIN_ROLES = Set.of("R04", "R09");
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Set<String> VISIBILITY_SCOPES = Set.of("SELF", "DEPARTMENT", "COLLEGE", "BUSINESS", "ALL");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private final ResultViewPeriodMapper mapper;

    public ResultViewPeriodService(ResultViewPeriodMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ResultViewPeriodSearchResponse list(CurrentUser user, ResultViewPeriodSearchCriteria criteria) {
        requireResultViewAdminRole(user);
        ResultViewPeriodSearchCriteria scoped = new ResultViewPeriodSearchCriteria(
                criteria.page(), criteria.pageSize(), criteria.evaluationYear(), criteria.collegeOrganizationCode(),
                criteria.departmentOrganizationCode(), criteria.visibilityScope(), criteria.activeYn(), criteria.keyword(),
                user.userId(), shouldRestrictOrganizationScope(user));
        return new ResultViewPeriodSearchResponse(mapper.listResultViewPeriods(scoped), Math.max(criteria.page(), 0),
                scoped.safeSize(), mapper.countResultViewPeriods(scoped));
    }

    @Transactional
    public ResultViewPeriodRow save(SaveResultViewPeriodRequest request, CurrentUser user, String requestId) {
        requireResultViewAdminRole(user);
        SaveResultViewPeriodRequest normalized = normalizeAndValidate(request);
        requireOrganizationScope(user, normalized.collegeOrganizationCode(), normalized.departmentOrganizationCode());
        ResultViewPeriodRow before = null;
        if (normalized.settingId() != null) {
            before = mapper.findResultViewPeriodById(normalized.settingId());
            if (before == null) throw new NotFoundException("결과조회기간 설정을 찾을 수 없습니다.");
        }
        if ("Y".equals(normalized.activeYn()) && mapper.countOverlappingResultViewPeriods(normalized.settingId(),
                normalized.evaluationYear(), normalized.collegeOrganizationCode(), normalized.departmentOrganizationCode(),
                normalized.visibilityScope(), normalized.viewStartAt(), normalized.viewEndAt()) > 0) {
            throw new ConflictException("동일 평가연도·소속·공개범위 기준의 활성 결과조회기간이 중복됩니다.");
        }
        ResultViewPeriodRow after = normalized.settingId() == null
                ? mapper.insertResultViewPeriod(normalized, user.userId())
                : mapper.updateResultViewPeriod(normalized, user.userId());
        recordChangeHistory(before, after, user.userId(), normalized.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public ResultViewPeriodDecision evaluateAccess(String organizationCode, String visibilityScope, LocalDateTime requestAt) {
        List<ValidationError> fields = new ArrayList<>();
        String normalizedOrganizationCode = normalized(organizationCode);
        String normalizedVisibilityScope = normalized(visibilityScope);
        if (!hasText(normalizedOrganizationCode)) fields.add(new ValidationError("organizationCode", "조회 대상 소속 코드를 입력하세요."));
        if (!VISIBILITY_SCOPES.contains(normalizedVisibilityScope)) fields.add(new ValidationError("visibilityScope", "공개 범위를 선택하세요."));
        if (requestAt == null) fields.add(new ValidationError("requestAt", "요청 일시를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("결과조회 공개기간 판정 요청이 올바르지 않습니다.", fields);
        }
        ResultViewPeriodRow row = mapper.findActiveResultViewPeriodForAccess(normalizedOrganizationCode, normalizedVisibilityScope, requestAt);
        if (row == null) {
            return ResultViewPeriodDecision.deny("결과조회 공개기간 또는 공개범위가 아닙니다.");
        }
        return ResultViewPeriodDecision.allow(row);
    }

    CurrentUser requireResultViewAdminRole(CurrentUser user) {
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

    private SaveResultViewPeriodRequest normalizeAndValidate(SaveResultViewPeriodRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String evaluationYear = trim(request.evaluationYear());
        String collegeOrganizationCode = normalized(request.collegeOrganizationCode());
        String departmentOrganizationCode = normalized(request.departmentOrganizationCode());
        String visibilityScope = normalized(request.visibilityScope());
        String activeYn = normalized(request.activeYn());
        String changeReason = trim(request.changeReason());
        if (!hasText(evaluationYear)) fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        else if (!EVALUATION_YEAR.matcher(evaluationYear).matches()) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!hasText(collegeOrganizationCode)) fields.add(new ValidationError("collegeOrganizationCode", "소속대학 코드를 입력하세요."));
        if (request.viewStartAt() == null) fields.add(new ValidationError("viewStartAt", "공개 시작일시를 입력하세요."));
        if (request.viewEndAt() == null) fields.add(new ValidationError("viewEndAt", "공개 종료일시를 입력하세요."));
        if (request.viewStartAt() != null && request.viewEndAt() != null && request.viewEndAt().isBefore(request.viewStartAt())) {
            fields.add(new ValidationError("viewEndAt", "종료일시는 시작일시 이후여야 합니다."));
        }
        if (visibilityScope == null || !VISIBILITY_SCOPES.contains(visibilityScope)) fields.add(new ValidationError("visibilityScope", "SELF, DEPARTMENT, COLLEGE, BUSINESS, ALL 중 하나를 선택하세요."));
        if (activeYn == null || !USE_FLAGS.contains(activeYn)) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("결과조회기간 저장 요청이 올바르지 않습니다.", fields);
        return new SaveResultViewPeriodRequest(request.settingId(), evaluationYear, collegeOrganizationCode,
                departmentOrganizationCode, request.viewStartAt(), request.viewEndAt(), visibilityScope,
                activeYn, changeReason);
    }

    private void recordChangeHistory(ResultViewPeriodRow before, ResultViewPeriodRow after, Long userId, String changeReason, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory("result_view_period_settings", String.valueOf(after.settingId()), changeType,
                    "setting", beforeValue, afterValue, userId, changeReason, requestId);
        }
    }

    private String summary(ResultViewPeriodRow row) {
        return row.evaluationYear() + ":" + row.collegeOrganizationCode() + ":" + row.departmentOrganizationCode()
                + ":" + row.visibilityScope();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
}
