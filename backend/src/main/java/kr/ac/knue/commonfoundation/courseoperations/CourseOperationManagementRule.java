package kr.ac.knue.commonfoundation.courseoperations;

/** DB-defined teacher mutability, requiredness and value type from the confirmed education rules. */
public record CourseOperationManagementRule(String teacherEditableYn, String requiredYn, String dataType) {
}
