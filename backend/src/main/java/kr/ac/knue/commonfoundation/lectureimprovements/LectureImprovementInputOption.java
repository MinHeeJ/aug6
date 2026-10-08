package kr.ac.knue.commonfoundation.lectureimprovements;

/** Existing FR-018 setting exposed as an input capability, not a client-defined field schema. */
public record LectureImprovementInputOption(String code, String name, String teacherEditablePart) {
}
