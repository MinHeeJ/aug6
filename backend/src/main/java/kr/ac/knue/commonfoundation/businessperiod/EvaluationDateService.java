package kr.ac.knue.commonfoundation.businessperiod;

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
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationDateService {
    private static final Set<String> ADMIN_ROLES = Set.of("R03", "R04", "R09");
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private final BusinessPeriodMapper mapper;

    public EvaluationDateService(BusinessPeriodMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationDateSearchResponse list(CurrentUser user, BusinessPeriodSearchCriteria criteria) {
        requireBusinessPeriodRole(user);
        BusinessPeriodSearchCriteria scoped = new BusinessPeriodSearchCriteria(
                criteria.page(), criteria.pageSize(), criteria.evaluationYear(), criteria.areaCode(),
                criteria.organizationCode(), criteria.userTypeCode(), criteria.activeYn(), criteria.keyword(),
                user.userId(), shouldRestrictOrganizationScope(user));
        return new EvaluationDateSearchResponse(mapper.listEvaluationDates(scoped), Math.max(criteria.page(), 0),
                scoped.safeSize(), mapper.countEvaluationDates(scoped));
    }

    @Transactional
    public BusinessPeriodSettingRow save(SaveEvaluationDateRequest request, CurrentUser user, String requestId) {
        requireBusinessPeriodRole(user);
        SaveEvaluationDateRequest normalized = normalizeAndValidate(request);
        requireOrganizationScope(user, normalized.organizationCode());
        BusinessPeriodSettingRow before = null;
        if (normalized.settingId() != null) {
            before = mapper.findEvaluationDateById(normalized.settingId());
            if (before == null) {
                throw new NotFoundException("평가일자 설정을 찾을 수 없습니다.");
            }
        }
        if ("Y".equals(normalized.activeYn()) && mapper.countOverlappingEvaluationDates(normalized.settingId(),
                normalized.evaluationYear(), normalized.areaCode(), normalized.organizationCode(), normalized.userTypeCode(),
                normalized.startAt(), normalized.endAt()) > 0) {
            throw new ConflictException("동일 평가연도·소속·영역·사용자유형의 활성 평가일자 기간이 중복됩니다.");
        }
        BusinessPeriodSettingRow after = normalized.settingId() == null
                ? mapper.insertEvaluationDate(normalized, user.userId())
                : mapper.updateEvaluationDate(normalized, user.userId());
        recordChangeHistory(before, after, user.userId(), normalized.changeReason(), requestId);
        return after;
    }

    private CurrentUser requireBusinessPeriodRole(CurrentUser user) {
        if (user == null) {
            throw new kr.ac.knue.commonfoundation.common.api.UnauthenticatedException();
        }
        if (user.roles().stream().anyMatch(ADMIN_ROLES::contains)) {
            return user;
        }
        throw new ForbiddenException();
    }

    private boolean shouldRestrictOrganizationScope(CurrentUser user) {
        return !user.roles().contains("R09") && (user.roles().contains("R03") || user.roles().contains("R04"));
    }

    private void requireOrganizationScope(CurrentUser user, String organizationCode) {
        if (!shouldRestrictOrganizationScope(user)) {
            return;
        }
        if (organizationCode == null || mapper.existsAuthorizedEvaluationOrganization(user.userId(), organizationCode) == 0) {
            throw new ForbiddenException();
        }
    }

    private SaveEvaluationDateRequest normalizeAndValidate(SaveEvaluationDateRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String evaluationYear = trim(request.evaluationYear());
        String areaCode = normalized(request.areaCode());
        String organizationCode = normalized(request.organizationCode());
        String userTypeCode = normalized(request.userTypeCode());
        String activeYn = normalized(request.activeYn());
        String changeReason = trim(request.changeReason());
        if (!hasText(evaluationYear)) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!EVALUATION_YEAR.matcher(evaluationYear).matches()) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (!hasText(organizationCode)) fields.add(new ValidationError("organizationCode", "소속/학과 코드를 입력하세요."));
        if (request.startAt() == null) fields.add(new ValidationError("startAt", "시작일시를 입력하세요."));
        if (request.endAt() == null) fields.add(new ValidationError("endAt", "종료일시를 입력하세요."));
        if (request.startAt() != null && request.endAt() != null && request.endAt().isBefore(request.startAt())) {
            fields.add(new ValidationError("endAt", "종료일시는 시작일시 이후여야 합니다."));
        }
        if (request.baseDate() == null) fields.add(new ValidationError("baseDate", "기준일자를 입력하세요."));
        if (!USE_FLAGS.contains(activeYn)) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (!hasText(changeReason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("평가일자 저장 요청이 올바르지 않습니다.", fields);
        }
        return new SaveEvaluationDateRequest(request.settingId(), evaluationYear, areaCode, organizationCode, userTypeCode,
                request.startAt(), request.endAt(), request.baseDate(), activeYn, changeReason);
    }

    private void recordChangeHistory(BusinessPeriodSettingRow before, BusinessPeriodSettingRow after, Long userId,
                                     String changeReason, String requestId) {
        String changeType = before == null ? "CREATE" : "UPDATE";
        String targetKey = String.valueOf(after.settingId());
        String beforeValue = before == null ? null : summary(before);
        String afterValue = summary(after);
        if (!Objects.equals(beforeValue, afterValue)) {
            mapper.insertChangeHistory("evaluation_date_settings", targetKey, changeType, "setting", beforeValue,
                    afterValue, userId, changeReason, requestId);
        }
    }

    private String summary(BusinessPeriodSettingRow row) {
        return row.evaluationYear() + ":" + nullToEmpty(row.areaCode()) + ":" + nullToEmpty(row.organizationCode())
                + ":" + nullToEmpty(row.userTypeCode());
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String normalized(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }
    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
