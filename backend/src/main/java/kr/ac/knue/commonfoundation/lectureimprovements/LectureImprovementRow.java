package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for a visible lecture-improvement header and its one-to-one detail values. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String achievementStatus,
        String attachmentRef,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
