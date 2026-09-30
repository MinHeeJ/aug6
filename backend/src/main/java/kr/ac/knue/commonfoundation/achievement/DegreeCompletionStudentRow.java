package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Read model for one persisted master or doctoral completion student. */
public record DegreeCompletionStudentRow(Long degreeCompletionStudentId, String degreeType, String studentName,
        String thesisTitle, LocalDate degreeAwardedDate) {
}
