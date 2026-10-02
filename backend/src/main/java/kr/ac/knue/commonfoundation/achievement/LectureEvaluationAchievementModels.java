package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transport shapes for the lecture-evaluation achievement API. These types keep API binding
 * separate from the persistence mapper while retaining the OpenAPI response field names.
 */
public final class LectureEvaluationAchievementModels {
    private LectureEvaluationAchievementModels() {
    }

    public record SearchCriteria(
            int page,
            int size,
            String managementNo,
            String teacherName,
            String managementItemCode,
            LocalDate occurredDateFrom,
            LocalDate occurredDateTo,
            String certificationStatus
    ) {
        public int safeSize() {
            return size == 20 || size == 50 || size == 100 ? size : 20;
        }
    }

    public record SearchResponse(
            List<Row> achievements,
            int page,
            int size,
            long totalElements
    ) {
    }

    public record Row(
            Long achievementId,
            String managementNo,
            Long teacherUserId,
            String teacherName,
            String organizationCode,
            String evaluationYear,
            String managementItemCode,
            LocalDate occurredDate,
            String achievementDetail,
            String certificationStatus,
            String attachmentRef,
            boolean occurredDateOutOfRangeWarning,
            LocalDateTime updatedAt
    ) {
    }
}
