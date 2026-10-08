package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.util.List;

/** Materializes the normalized header and its mandatory lecture-improvement detail. */
public record LectureImprovementRow(
        Long achievementId,
        Long teacherUserId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementContent,
        Integer academicYear,
        Integer semester,
        String achievementStatus,
        List<String> attachmentIds) {
}
