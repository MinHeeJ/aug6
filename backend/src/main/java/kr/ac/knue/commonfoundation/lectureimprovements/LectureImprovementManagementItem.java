package kr.ac.knue.commonfoundation.lectureimprovements;

/** Existing education management-item input rules, not a new configuration store. */
public record LectureImprovementManagementItem(
        String managementItemCode, String managementItemName, String requiredYn,
        String dataType, String teacherEditableYn, String evaluationYear) {
}
