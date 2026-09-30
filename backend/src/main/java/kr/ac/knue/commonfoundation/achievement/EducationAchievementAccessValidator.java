package kr.ac.knue.commonfoundation.achievement;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Enforces the shared authorization, period, finalization-lock, warning, and status-transition invariants for education achievements.
 */
@Component
public class EducationAchievementAccessValidator {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04", "R09");
    private static final Set<String> ACHIEVEMENT_TYPES = Set.of(
            "LECTURE_EVALUATION",
            "LECTURE",
            "STUDENT_GUIDANCE",
            "DEGREE_COMPLETION"
    );
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "DRAFTING", Set.of("SUBMITTED"),
            "SUBMITTED", Set.of("DEPARTMENT_CONFIRMED", "DEPARTMENT_REJECTED"),
            "DEPARTMENT_REJECTED", Set.of("SUBMITTED"),
            "DEPARTMENT_CONFIRMED", Set.of("CERTIFIED", "CERTIFICATION_RETURNED"),
            "CERTIFICATION_RETURNED", Set.of("SUBMITTED"),
            "CERTIFIED", Set.of("EVALUATION_CONFIRMED"),
            "EVALUATION_CONFIRMED", Set.of("CERTIFIED")
    );

    private final EducationAchievementAccessMapper mapper;
    private final Clock clock;

    @Autowired
    public EducationAchievementAccessValidator(EducationAchievementAccessMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    EducationAchievementAccessValidator(EducationAchievementAccessMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Requires role, data scope, active input period, and no finalization lock before a write, returning only the allowed date warning.
     */
    public EducationAchievementAccessDecision validateWrite(CurrentUser user, AchievementWriteContext context) {
        requireAuthenticatedRole(user);
        validateContext(context);
        requireDataScope(user, context);
        LocalDateTime processedAt = LocalDateTime.now(clock);
        if (mapper.countActiveInputPeriod(
                context.evaluationYear(),
                context.organizationCode(),
                processedAt
        ) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 현재 교육영역 입력기간이 아닙니다.");
        }
        if (mapper.countEvaluationFinalizationLock(context.targetUserId(), context.evaluationYear()) > 0
                || "EVALUATION_CONFIRMED".equals(normalize(context.certificationStatus()))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }
        if (mapper.countOccurredDateInEvaluationPeriod(
                context.evaluationYear(),
                context.organizationCode(),
                context.occurredDate()
        ) == 0) {
            return new EducationAchievementAccessDecision(List.of("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD"));
        }
        return new EducationAchievementAccessDecision(List.of());
    }

    /**
     * Validates the canonical status graph and returns the complete immutable history payload for later transactional persistence.
     */
    public EducationAchievementStatusTransition prepareStatusTransition(
            String achievementType,
            Long achievementId,
            String previousStatus,
            String nextStatus,
            String actionType,
            String reasonCode,
            String opinion,
            Long processedBy,
            LocalDateTime processedAt
    ) {
        List<ValidationError> fields = new ArrayList<>();
        String normalizedType = normalize(achievementType);
        String normalizedPrevious = normalize(previousStatus);
        String normalizedNext = normalize(nextStatus);
        String normalizedAction = normalize(actionType);
        String normalizedReason = trimToNull(reasonCode);
        String normalizedOpinion = trimToNull(opinion);

        if (!ACHIEVEMENT_TYPES.contains(normalizedType)) {
            fields.add(new ValidationError("achievementType", "지원하지 않는 교육영역 실적 유형입니다."));
        }
        if (achievementId == null || achievementId <= 0) {
            fields.add(new ValidationError("achievementId", "실적 식별자가 올바르지 않습니다."));
        }
        if (normalizedAction == null) {
            fields.add(new ValidationError("actionType", "처리구분을 선택하세요."));
        }
        if (processedBy == null || processedBy <= 0) {
            fields.add(new ValidationError("processedBy", "처리자 식별자가 올바르지 않습니다."));
        }
        if (processedAt == null) {
            fields.add(new ValidationError("processedAt", "처리일시가 필요합니다."));
        }
        if (!isAllowedTransition(normalizedPrevious, normalizedNext)) {
            fields.add(new ValidationError("nextStatus", "INVALID_STATE_TRANSITION: 허용되지 않은 상태 전이입니다."));
        }
        if (("DEPARTMENT_REJECTED".equals(normalizedNext)
                || "CERTIFICATION_RETURNED".equals(normalizedNext))
                && normalizedReason == null
                && normalizedOpinion == null) {
            fields.add(new ValidationError("reasonCode", "반려 시 사유 또는 의견을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 상태 전이 요청이 올바르지 않습니다.", fields);
        }
        return new EducationAchievementStatusTransition(
                normalizedType,
                achievementId,
                normalizedPrevious,
                normalizedNext,
                normalizedAction,
                normalizedReason,
                normalizedOpinion,
                processedBy,
                processedAt
        );
    }

    private void requireAuthenticatedRole(CurrentUser user) {
        if (user == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireDataScope(CurrentUser user, AchievementWriteContext context) {
        if (user.roles().contains("R01") && !user.userId().equals(context.targetUserId())) {
            throw new ForbiddenException();
        }
        if (user.roles().contains("R02")
                && mapper.countAuthorizedOrganization(user.userId(), context.organizationCode()) == 0) {
            throw new ForbiddenException();
        }
    }

    private void validateContext(AchievementWriteContext context) {
        List<ValidationError> fields = new ArrayList<>();
        if (context == null) {
            throw new BusinessValidationException(
                    "교육영역 실적 검증 정보가 필요합니다.",
                    List.of(new ValidationError("context", "실적 검증 정보가 필요합니다."))
            );
        }
        if (context.targetUserId() == null || context.targetUserId() <= 0) {
            fields.add(new ValidationError("targetUserId", "실적 소유자를 선택하세요."));
        }
        if (normalize(context.evaluationYear()) == null || !context.evaluationYear().trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (normalize(context.organizationCode()) == null) {
            fields.add(new ValidationError("organizationCode", "소속 조직코드가 필요합니다."));
        }
        if (context.occurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 검증 정보가 올바르지 않습니다.", fields);
        }
    }

    private boolean isAllowedTransition(String previousStatus, String nextStatus) {
        return previousStatus != null
                && nextStatus != null
                && ALLOWED_TRANSITIONS.getOrDefault(previousStatus, Set.of()).contains(nextStatus);
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
