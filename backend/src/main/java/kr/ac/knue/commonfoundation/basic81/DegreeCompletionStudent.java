package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;

/** Read model for a degree-completion student detail without exposing unrelated personal data. */
public record DegreeCompletionStudent(
        Long degreeCompletionStudentId,
        Long achievementId,
        String degreeType,
        String studentName,
        String thesisTitle,
        LocalDate degreeAwardedDate) {
}
