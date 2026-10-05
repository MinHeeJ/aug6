package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;

/**
 * Represents a persisted lecture-improvement record as returned by the
 * education-achievement header and its detail row.
 */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        String attachmentIds) {
}
