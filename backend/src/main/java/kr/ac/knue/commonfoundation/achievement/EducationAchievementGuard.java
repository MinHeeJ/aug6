package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;

/**
 * Applies the shared authorization and lifecycle invariants for education achievements before
 * feature-specific services mutate headers, details, attachments, or history rows.
 */
@Service
public class EducationAchievementGuard {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04");
    private static final Set<String> REJECTION_ACTIONS = Set.of(
            "DEPARTMENT_REJECT",
            "CERTIFICATION_REJECT"
    );
    private static final Map<String, String> NEXT_STATUS_BY_ACTION = Map.of(
            "SUBMIT", "SUBMITTED",
            "DEPARTMENT_CONFIRM", "DEPARTMENT_CONFIRMED",
            "DEPARTMENT_REJECT", "DEPARTMENT_REJECTED",
            "CERTIFY", "CERTIFIED",
            "CERTIFICATION_REJECT", "CERTIFICATION_REJECTED",
            "CONFIRM_EVALUATION", "EVALUATION_CONFIRMED",
            "CANCEL_EVALUATION_CONFIRMATION", "CERTIFIED",
            "DELETE", "DELETED"
    );
    private final EducationAchievementGuardMapper mapper;

    public EducationAchievementGuard(EducationAchievementGuardMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Enforces role, data-scope, active input-period, and finalization-lock checks. An
     * out-of-period occurrence is deliberately returned as a warning because the contract allows
     * the record to be saved after the user is informed.
     */
    public EducationAchievementGuardResult validateMutation(
            CurrentUser user,
            EducationAchievementCommandContext context
    ) {
        requireEligibleUser(user);
        validateContext(context);
        requireDataScope(user, context);
        if (mapper.existsOpenInputPeriod(
                context.evaluationYear(),
                context.organizationCode(),
                LocalDateTime.now()
        ) == 0) {
            throw new ConflictException("현재 교육영역 실적 입력기간이 아닙니다.");
        }
        if ("EVALUATION_CONFIRMED".equals(context.currentStatus())
                || mapper.existsEvaluationFinalization(
                        context.targetUserId(),
                        context.evaluationYear()
                ) > 0) {
            throw new ConflictException("EVALUATION_CONFIRMED: 평가확정된 실적은 수정하거나 삭제할 수 없습니다.");
        }
        boolean occurredDateOutOfRangeWarning = mapper.existsOccurredDateWithinEvaluationPeriod(
                context.evaluationYear(),
                context.organizationCode(),
                context.occurredDate()
        ) == 0;
        return new EducationAchievementGuardResult(occurredDateOutOfRangeWarning);
    }

    /**
     * Validates the only permitted achievement status transitions and returns the history payload
     * that a feature service must insert atomically with its state update.
     */
    public EducationAchievementTransition validateTransition(
            String currentStatus,
            String actionType,
            String reasonCode,
            String opinion
    ) {
        String normalizedCurrentStatus = normalize(currentStatus);
        String normalizedActionType = normalize(actionType);
        String nextStatus = NEXT_STATUS_BY_ACTION.get(normalizedActionType);
        if (nextStatus == null || !isAllowedTransition(normalizedCurrentStatus, normalizedActionType)) {
            throw new ConflictException("허용되지 않은 교육영역 실적 상태 전이입니다.");
        }
        if (REJECTION_ACTIONS.contains(normalizedActionType)
                && (isBlank(reasonCode) || isBlank(opinion))) {
            throw new BusinessValidationException(
                    "반려 처리에는 사유와 의견이 필요합니다.",
                    List.of(
                            new ValidationError("reasonCode", "반려 사유를 입력하세요."),
                            new ValidationError("opinion", "반려 의견을 입력하세요.")
                    )
            );
        }
        return new EducationAchievementTransition(
                normalizedCurrentStatus,
                nextStatus,
                normalizedActionType,
                normalizeNullable(reasonCode),
                normalizeNullable(opinion)
        );
    }

    private void requireEligibleUser(CurrentUser user) {
        if (user == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void validateContext(EducationAchievementCommandContext context) {
        List<ValidationError> fields = new java.util.ArrayList<>();
        if (context == null) {
            throw new BusinessValidationException(
                    "교육영역 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("body", "저장 요청을 입력하세요."))
            );
        }
        if (context.targetUserId() == null || context.targetUserId() <= 0) {
            fields.add(new ValidationError("targetUserId", "실적 소유 교원을 선택하세요."));
        }
        if (isBlank(context.evaluationYear()) || !context.evaluationYear().trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        }
        if (isBlank(context.organizationCode())) {
            fields.add(new ValidationError("organizationCode", "소속 조직을 선택하세요."));
        }
        if (context.occurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private void requireDataScope(CurrentUser user, EducationAchievementCommandContext context) {
        if (!user.roles().contains("R02") && !user.roles().contains("R04")) {
            if (!user.userId().equals(context.targetUserId())) {
                throw new ForbiddenException();
            }
            return;
        }
        if (mapper.existsAuthorizedEvaluationOrganization(user.userId(), context.organizationCode()) == 0) {
            throw new ForbiddenException();
        }
    }

    private boolean isAllowedTransition(String currentStatus, String actionType) {
        return switch (currentStatus) {
            case "DRAFT" -> "SUBMIT".equals(actionType) || "DELETE".equals(actionType);
            case "SUBMITTED" -> "DEPARTMENT_CONFIRM".equals(actionType)
                    || "DEPARTMENT_REJECT".equals(actionType);
            case "DEPARTMENT_REJECTED" -> "SUBMIT".equals(actionType);
            case "DEPARTMENT_CONFIRMED" -> "CERTIFY".equals(actionType)
                    || "CERTIFICATION_REJECT".equals(actionType);
            case "CERTIFICATION_REJECTED" -> "SUBMIT".equals(actionType);
            case "CERTIFIED" -> "CONFIRM_EVALUATION".equals(actionType)
                    || "DELETE".equals(actionType);
            case "EVALUATION_CONFIRMED" -> "CANCEL_EVALUATION_CONFIRMATION".equals(actionType);
            default -> false;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
