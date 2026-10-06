package kr.ac.knue.commonfoundation.lectureimprovements;

/** Configuration used to enforce the selected management item's field constraints. */
public record LectureImprovementManagementItem(
        String code, String name, String requiredYn, String dataType, String teacherEditableYn) {
}
