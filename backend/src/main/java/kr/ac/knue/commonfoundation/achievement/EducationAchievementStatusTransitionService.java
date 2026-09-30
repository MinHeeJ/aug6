package kr.ac.knue.commonfoundation.achievement;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/**
 * Validates CMN-202/203 and records a complete status-history payload for a permitted education achievement transition.
 * Persistence is delegated so each later achievement service can record the history atomically with its own row update.
 */
public class EducationAchievementStatusTransitionService {
    private final EducationAchievementStatusHistoryPort historyPort;
    private final Clock clock;

    public EducationAchievementStatusTransitionService(EducationAchievementStatusHistoryPort historyPort, Clock clock) {
        this.historyPort = historyPort;
        this.clock = clock;
    }

    /**
     * Rejects invalid graph edges and missing rejection reasons before recording an immutable transition audit event.
     */
    public EducationAchievementStatusTransition transition(String achievementType, Long achievementId,
                                                            EducationAchievementStatus previousStatus,
                                                            EducationAchievementStatus nextStatus,
                                                            String transitionReason, Long processedBy,
                                                            String requestId) {
        validateRequiredValues(achievementType, achievementId, previousStatus, nextStatus, processedBy);
        if (!previousStatus.canTransitionTo(nextStatus)) {
            throw new ConflictException("INVALID_STATE_TRANSITION: 허용되지 않은 업적 상태 전이입니다.");
        }
        String normalizedReason = trimToNull(transitionReason);
        if (nextStatus.requiresReason() && normalizedReason == null) {
            throw new BusinessValidationException("반려 사유 또는 의견을 입력하세요.",
                    List.of(new ValidationError("transitionReason", "반려 사유 또는 의견을 입력하세요.")));
        }
        EducationAchievementStatusTransition transition = new EducationAchievementStatusTransition(
                achievementType.trim(), achievementId, previousStatus, nextStatus, normalizedReason, processedBy,
                LocalDateTime.now(clock), trimToNull(requestId));
        historyPort.record(transition);
        return transition;
    }

    private void validateRequiredValues(String achievementType, Long achievementId,
                                        EducationAchievementStatus previousStatus,
                                        EducationAchievementStatus nextStatus, Long processedBy) {
        java.util.ArrayList<ValidationError> fields = new java.util.ArrayList<>();
        if (trimToNull(achievementType) == null) fields.add(new ValidationError("achievementType", "실적 유형을 입력하세요."));
        if (achievementId == null || achievementId <= 0) fields.add(new ValidationError("achievementId", "실적을 선택하세요."));
        if (previousStatus == null) fields.add(new ValidationError("previousStatus", "이전 상태가 필요합니다."));
        if (nextStatus == null) fields.add(new ValidationError("nextStatus", "변경 상태를 선택하세요."));
        if (processedBy == null || processedBy <= 0) fields.add(new ValidationError("processedBy", "처리자가 필요합니다."));
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("상태 전이 요청이 올바르지 않습니다.", fields);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
