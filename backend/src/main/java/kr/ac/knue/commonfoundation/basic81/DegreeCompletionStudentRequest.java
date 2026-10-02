package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;

/** Input fields for one degree-completion student detail saved with its header. */
public record DegreeCompletionStudentRequest(
        String degreeType,
        String studentName,
        String thesisTitle,
        LocalDate degreeAwardedDate) {
}
