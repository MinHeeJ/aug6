package kr.ac.knue.commonfoundation.courseoperations;

/** DB-backed teacher-editable management item option for this feature. */
public record CourseOperationItemOption(String code, String name, String evaluationYear, String teacherEditablePart) {
}
