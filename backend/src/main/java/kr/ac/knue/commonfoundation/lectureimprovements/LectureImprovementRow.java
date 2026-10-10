package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Materialized parent and detail snapshot, including immutable owner and evaluation identity. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String performanceContent,
        String academicYear,
        String semester,
        String achievementStatus,
        String attachmentRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
