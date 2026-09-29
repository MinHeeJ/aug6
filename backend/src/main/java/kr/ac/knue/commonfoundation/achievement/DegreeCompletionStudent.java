package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Read model for one student supervised through a degree completion achievement. */
public record DegreeCompletionStudent(Long degreeCompletionStudentId, String degreeType, String studentName,
        String thesisTitle, LocalDate degreeAwardedDate) {
}
