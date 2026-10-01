package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;

/**
 * Enforces the common mutation gate for education achievements in the required order: role and
 * data scope, active input period, evaluation finalization lock, then non-blocking date warning.
 */
@Service
public class EducationAchievementAccessValidator {
    private static final String EDUCATION_AREA_CODE = "EDUCATION";
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04");
    private static final Pattern EVALUATION_YEAR_PATTERN = Pattern.compile("^[0-9]{4}$");

    private final EducationAchievementAccessMapper mapper;

    public EducationAchievementAccessValidator(EducationAchievementAccessMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Validates persistent access and lock facts before a create, update, delete, or attachment
     * mutation. An occurrence date outside the evaluation period is returned as a warning because
     * the business contract explicitly permits the save.
     */
    public EducationAchievementValidationResult validateMutation(
            CurrentUser actor,
            EducationAchievementMutationContext context
    ) {
        EducationAchievementMutationContext normalized = normalizeAndValidate(context);
        requireAuthenticatedRole(actor);
        requireDataScope(actor, normalized);
        requireActiveInputPeriod(normalized);
        requireNotEvaluationConfirmed(normalized);
        boolean occurredDateWarning = mapper.countEvaluationPeriodContainingDate(
                normalized.evaluationYear(),
                EDUCATION_AREA_CODE,
                normalized.organizationCode(),
                normalized.occurredDate()
        ) == 0;
        return new EducationAchievementValidationResult(occurredDateWarning);
    }

    private EducationAchievementMutationContext normalizeAndValidate(
            EducationAchievementMutationContext context
    ) {
        if (context == null) {
            throw new BusinessValidationException(
                    "교육영역 실적 처리 대상이 올바르지 않습니다.",
                    List.of(new ValidationError("context", "실적 처리 대상을 입력하세요."))
            );
        }
        String evaluationYear = trim(context.evaluationYear());
        String organizationCode = normalizeUpper(context.organizationCode());
        LocalDateTime requestedAt = context.requestedAt() == null
                ? LocalDateTime.now()
                : context.requestedAt();
        java.util.ArrayList<ValidationError> fields = new java.util.ArrayList<>();
        if (context.targetUserId() == null || context.targetUserId() <= 0) {
            fields.add(new ValidationError("targetUserId", "대상 교원을 입력하세요."));
        }
        if (evaluationYear == null || !EVALUATION_YEAR_PATTERN.matcher(evaluationYear).matches()) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (organizationCode == null) {
            fields.add(new ValidationError("organizationCode", "대상 소속을 입력하세요."));
        }
        if (context.occurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 처리 요청이 올바르지 않습니다.", fields);
        }
        return new EducationAchievementMutationContext(
                context.targetUserId(),
                evaluationYear,
                organizationCode,
                context.occurredDate(),
                requestedAt
        );
    }

    private void requireAuthenticatedRole(CurrentUser actor) {
        if (actor == null) {
            throw new UnauthenticatedException();
        }
        if (actor.roles() == null || actor.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireDataScope(CurrentUser actor, EducationAchievementMutationContext context) {
        if (actor.roles().contains("R04")) {
            return;
        }
        if (actor.roles().contains("R01") && actor.userId().equals(context.targetUserId())) {
            return;
        }
        if (actor.roles().contains("R02") && mapper.countDepartmentScopedTarget(
                actor.userId(),
                context.targetUserId(),
                context.organizationCode()
        ) > 0) {
            return;
        }
        throw new ForbiddenException();
    }

    private void requireActiveInputPeriod(EducationAchievementMutationContext context) {
        if (mapper.countActiveInputPeriods(
                context.evaluationYear(),
                EDUCATION_AREA_CODE,
                context.organizationCode(),
                context.requestedAt()
        ) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 현재 활성 입력기간이 아닙니다.");
        }
    }

    private void requireNotEvaluationConfirmed(EducationAchievementMutationContext context) {
        if (mapper.countConfirmedFinalizations(context.targetUserId(), context.evaluationYear()) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정된 실적은 변경할 수 없습니다.");
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }
}
