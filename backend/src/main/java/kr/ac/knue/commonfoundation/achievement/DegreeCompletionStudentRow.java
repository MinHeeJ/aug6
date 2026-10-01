package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Read model for a persisted master's or doctoral completion student detail. */
public record DegreeCompletionStudentRow(
        Long degreeCompletionStudentId,
        String degreeType,
        String studentName,
        String thesisTitle,
        LocalDate degreeAwardedDate
) {
}
