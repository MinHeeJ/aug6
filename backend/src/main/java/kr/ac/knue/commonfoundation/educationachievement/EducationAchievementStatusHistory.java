package kr.ac.knue.commonfoundation.educationachievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/** Immutable audit representation of one education-achievement status transition. */
public record EducationAchievementStatusHistory(
        Long historyId,
        String previousStatus,
        String nextStatus,
        Long processedBy,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime processedAt,
        String reason) {
}
