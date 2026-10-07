package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;

/** Joined header/detail view with opaque attachment references, never filesystem paths. */
public record CourseOperationRow(
        Long achievementId,
        String managementNo,
        Long teacherUserId,
        String teacherName,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementStatus,
        String performanceDetails,
        List<String> attachmentIds) {
}
