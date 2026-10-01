package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Component;

/**
 * Defines the only permitted certification-state graph and rejection-reason invariant shared by
 * the four education achievement types before state history is persisted transactionally.
 */
@Component
public class EducationAchievementStatusTransitionPolicy {
    private static final Map<String, Map<String, String>> NEXT_STATUS_BY_ACTION = Map.of(
            "DRAFT", Map.of("SUBMIT", "SUBMITTED"),
            "SUBMITTED", Map.of(
                    "DEPARTMENT_CONFIRM", "DEPARTMENT_CONFIRMED",
                    "DEPARTMENT_REJECT", "DEPARTMENT_REJECTED"
            ),
            "DEPARTMENT_REJECTED", Map.of("SUBMIT", "SUBMITTED"),
            "DEPARTMENT_CONFIRMED", Map.of(
                    "CERTIFY", "CERTIFIED",
                    "CERTIFICATION_REJECT", "CERTIFICATION_REJECTED"
            ),
            "CERTIFICATION_REJECTED", Map.of("SUBMIT", "SUBMITTED"),
            "CERTIFIED", Map.of("FINALIZE", "EVALUATION_CONFIRMED"),
            "EVALUATION_CONFIRMED", Map.of("CANCEL_FINALIZATION", "CERTIFIED")
    );

    /**
     * Validates a requested transition and returns the complete history payload including its
     * processor and timestamp; rejected transitions produce no payload for persistence.
     */
    public EducationAchievementStatusTransition transition(
            String currentStatus,
            String actionType,
            String reasonCode,
            String opinion,
            Long processedBy,
            LocalDateTime processedAt
    ) {
        String previousStatus = normalizeUpper(currentStatus);
        String normalizedAction = normalizeUpper(actionType);
        String normalizedReason = trim(reasonCode);
        String normalizedOpinion = trim(opinion);
        if (processedBy == null || processedBy <= 0) {
            throw new BusinessValidationException(
                    "상태 처리 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("processedBy", "처리자를 확인할 수 없습니다."))
            );
        }
        String nextStatus = NEXT_STATUS_BY_ACTION
                .getOrDefault(previousStatus, Map.of())
                .get(normalizedAction);
        if (nextStatus == null) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 허용되지 않은 상태 전이입니다.");
        }
        if (("DEPARTMENT_REJECTED".equals(nextStatus) || "CERTIFICATION_REJECTED".equals(nextStatus))
                && normalizedReason == null
                && normalizedOpinion == null) {
            throw new BusinessValidationException(
                    "반려 처리에는 사유 또는 의견이 필요합니다.",
                    List.of(new ValidationError("reasonCode", "반려 사유 또는 의견을 입력하세요."))
            );
        }
        return new EducationAchievementStatusTransition(
                previousStatus,
                nextStatus,
                normalizedAction,
                normalizedReason,
                normalizedOpinion,
                processedBy,
                processedAt == null ? LocalDateTime.now() : processedAt
        );
    }

    private String normalizeUpper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? "" : trimmed.toUpperCase();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
