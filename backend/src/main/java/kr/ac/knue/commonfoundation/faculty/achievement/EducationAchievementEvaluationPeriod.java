package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;

/** Applicable evaluation-date boundary used only to emit the non-blocking occurred-date warning. */
public record EducationAchievementEvaluationPeriod(LocalDate startDate, LocalDate endDate) {
}
