package kr.ac.knue.commonfoundation.achievement;
/** One persisted guided-student detail; student identity is unique inside its achievement. */
public record StudentGuidanceStudentRow(Long studentGuidanceStudentId, String studentNo, String studentName, String guidanceType) {}
