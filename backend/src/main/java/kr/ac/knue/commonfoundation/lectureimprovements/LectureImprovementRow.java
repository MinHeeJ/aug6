package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for a visible lecture-improvement achievement and its immutable lifecycle state. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        List<String> attachmentIds,
        String achievementStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
