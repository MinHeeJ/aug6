package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;

/** Approved individual input; owner, evaluation year and status are server-controlled. */
public record EmploymentRateAchievementRequest(
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}
