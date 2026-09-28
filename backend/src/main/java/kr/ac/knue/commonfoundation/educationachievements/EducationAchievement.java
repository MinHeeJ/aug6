package kr.ac.knue.commonfoundation.educationachievements;

import java.time.LocalDate;

/** Read model shared by the education achievement list endpoints. */
public record EducationAchievement(Long achievementId, String managementItemCode, LocalDate occurredOn, String detailContent, String status, boolean deleted) {
}
