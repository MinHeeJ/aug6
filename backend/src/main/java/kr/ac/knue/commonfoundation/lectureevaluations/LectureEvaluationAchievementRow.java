package kr.ac.knue.commonfoundation.lectureevaluations;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model returned by lecture-evaluation search and save operations. */
public record LectureEvaluationAchievementRow(Long achievementId, String managementNo, String teacherName,
        String managementItemCode, String organizationCode, LocalDate occurredDate, String achievementDetail,
        String certificationStatus, String attachmentRef, LocalDateTime updatedAt, Long updatedBy) {
}
