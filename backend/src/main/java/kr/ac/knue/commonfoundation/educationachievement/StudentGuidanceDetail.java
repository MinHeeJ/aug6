package kr.ac.knue.commonfoundation.educationachievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/** Represents one persisted student-guidance period belonging to an education achievement. */
public record StudentGuidanceDetail(
        Long studentGuidanceDetailId,
        String studentName,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate guidanceStartDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate guidanceEndDate,
        Integer studentCount) {
}
