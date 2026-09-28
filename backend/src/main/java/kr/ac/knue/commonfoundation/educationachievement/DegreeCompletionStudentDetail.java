package kr.ac.knue.commonfoundation.educationachievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/** Represents a persisted master or doctoral completion detail returned with its achievement. */
public record DegreeCompletionStudentDetail(
        Long degreeCompletionStudentDetailId,
        String degreeType,
        String studentName,
        String thesisTitle,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate degreeAwardedOn) {
}