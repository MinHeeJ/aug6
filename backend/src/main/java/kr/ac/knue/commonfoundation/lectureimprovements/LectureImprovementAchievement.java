package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model returned by lecture-improvement list and detail operations. */
public record LectureImprovementAchievement(
        Long achievementId,
        Long targetUserId,
        String teacherName,
        String managementNo,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        String achievementStatus,
        String attachmentRefs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
