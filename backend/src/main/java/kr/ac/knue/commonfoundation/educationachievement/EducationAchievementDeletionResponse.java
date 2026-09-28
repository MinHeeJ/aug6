package kr.ac.knue.commonfoundation.educationachievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Describes an auditable logical deletion without exposing the physical storage implementation.
 */
public record EducationAchievementDeletionResponse(
        Long achievementId,
        String deletedYn,
        Long deletedBy,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime deletedAt,
        EducationAchievementChangeHistory changeHistory) {
}
