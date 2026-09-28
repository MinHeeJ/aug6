package kr.ac.knue.commonfoundation.educationachievement;

import java.time.LocalDate;

/** Carries the student-guidance fields submitted as part of an individual achievement save. */
public record StudentGuidanceDetailRequest(
        String studentName,
        LocalDate guidanceStartDate,
        LocalDate guidanceEndDate,
        Integer studentCount) {
}
