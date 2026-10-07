package kr.ac.knue.commonfoundation.courseoperations;

/** Source-backed management metadata for teacher forms, without opening admin APIs. */
public record CourseOperationManagementItem(
        String managementItemCode, String managementItemName, String evaluationYear,
        String requiredYn, String dataType, String teacherEditableYn) {
}
