package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;

/** Internal command value used to insert the common education-achievement source row. */
public record CourseOperationCreateRow(
        String managementNo,
        Long teacherUserId,
        String organizationCode,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String attachmentRefs,
        Long createdBy) {
}
