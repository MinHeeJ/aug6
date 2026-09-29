package kr.ac.knue.commonfoundation.lectureachievements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model returned by lecture-achievement search and save operations. */
public record LectureAchievementRow(Long achievementId, String managementNo, String teacherName,
        String managementItemCode, String organizationCode, LocalDate occurredDate, String achievementDetail,
        String certificationStatus, String attachmentRef, LocalDateTime updatedAt, Long updatedBy) {
}
