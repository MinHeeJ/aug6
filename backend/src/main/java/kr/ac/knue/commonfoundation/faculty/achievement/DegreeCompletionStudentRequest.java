package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;

public record DegreeCompletionStudentRequest(String degreeType, String studentName, String thesisTitle, LocalDate degreeAwardedDate) { }