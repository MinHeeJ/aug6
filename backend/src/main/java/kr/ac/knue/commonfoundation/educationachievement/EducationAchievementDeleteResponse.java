package kr.ac.knue.commonfoundation.educationachievement;

import java.time.OffsetDateTime;

/**
 * Returns the logical-deletion audit evidence for an education achievement without exposing storage details.
 */
public record EducationAchievementDeleteResponse(
        Long achievementId,
        String deletedYn,
        OffsetDateTime deletedAt,
        Long deletedBy,
        String deleteReason,
        Audit audit) {

    /** Captures the immutable common change-history values correlated by the client request identifier. */
    public record Audit(
            String changeType,
            String beforeValue,
            String afterValue,
            Long changedBy,
            String requestId) {
    }
}
