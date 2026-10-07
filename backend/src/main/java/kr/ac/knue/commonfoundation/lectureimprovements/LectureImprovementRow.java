package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Joined source and detail values returned without exposing credentials or internal scope records. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long userId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        String attachmentRef,
        String performanceContent,
        String academicYear,
        String semester,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
