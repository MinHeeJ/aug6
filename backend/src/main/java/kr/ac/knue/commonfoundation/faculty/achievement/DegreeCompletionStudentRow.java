package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;

public record DegreeCompletionStudentRow(String degreeType, String studentName, String thesisTitle, LocalDate degreeAwardedDate) { }