package kr.ac.knue.commonfoundation.achievement;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/**
 * Applies the shared education-achievement mutation gate in the required order: role, data scope,
 * input period, final-evaluation lock, then occurrence-date warning. It prevents future feature
 * services from bypassing a blocking business condition while preserving a date warning as non-blocking.
 */
public class EducationAchievementValidationService {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04");
    private final EducationAchievementValidationPort validationPort;
    private final Clock clock;

    public EducationAchievementValidationService(EducationAchievementValidationPort validationPort) {
        this(validationPort, Clock.systemUTC());
    }

    public EducationAchievementValidationService(EducationAchievementValidationPort validationPort, Clock clock) {
        this.validationPort = validationPort;
        this.clock = clock;
    }

    /**
     * Validates one create, update, transition, or attachment mutation before persistence begins.
     * A date outside the evaluation period yields a warning rather than preventing the permitted save.
     */
    public EducationAchievementValidationResult validateMutation(EducationAchievementValidationContext context) {
        validateRequiredContext(context);
        CurrentUser user = requireAllowedRole(context.user());
        if (!validationPort.hasDataScope(user.userId(), context.ownerUserId(), context.organizationCode(),
                context.evaluationUnitCode())) {
            throw new ForbiddenException();
        }
        if (!validationPort.hasActiveInputPeriod(context.evaluationYear(), context.organizationCode(),
                context.evaluationUnitCode(), LocalDateTime.now(clock))) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간 안에서만 처리할 수 있습니다.");
        }
        if (validationPort.hasEvaluationResultLock(context.evaluationYear(), context.organizationCode(),
                context.evaluationUnitCode())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }
        boolean occurrenceDateWarning = !validationPort.isWithinEvaluationDate(context.evaluationYear(),
                context.organizationCode(), context.occurrenceDate());
        return new EducationAchievementValidationResult(occurrenceDateWarning);
    }

    private CurrentUser requireAllowedRole(CurrentUser user) {
        if (user == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private void validateRequiredContext(EducationAchievementValidationContext context) {
        if (context == null) {
            throw new BusinessValidationException("교육영역 실적 검증 정보가 필요합니다.",
                    List.of(new ValidationError("context", "검증 정보를 입력하세요.")));
        }
        java.util.ArrayList<ValidationError> fields = new java.util.ArrayList<>();
        if (context.ownerUserId() == null || context.ownerUserId() <= 0) fields.add(new ValidationError("ownerUserId", "실적 소유자를 입력하세요."));
        if (!hasText(context.evaluationYear())) fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        if (!hasText(context.organizationCode())) fields.add(new ValidationError("organizationCode", "소속 코드를 입력하세요."));
        if (context.occurrenceDate() == null) fields.add(new ValidationError("occurrenceDate", "업적발생일을 입력하세요."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 검증 정보가 올바르지 않습니다.", fields);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
