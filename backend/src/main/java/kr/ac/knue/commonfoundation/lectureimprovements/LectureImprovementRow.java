package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.util.List;

/** Materialized header and one-to-one teaching detail, including immutable evaluation identity. */
public record LectureImprovementRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        String academicYear,
        String semester,
        String achievementContent,
        List<String> attachmentIds) {
}
