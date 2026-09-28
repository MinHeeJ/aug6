package kr.ac.knue.commonfoundation.educationachievement;

import java.time.LocalDate;

/** Captures one student detail belonging to a degree-completion achievement. */
public record DegreeCompletionStudentDetailRequest(
        String degreeType,
        String studentName,
        String thesisTitle,
        LocalDate degreeAwardedOn) {
}