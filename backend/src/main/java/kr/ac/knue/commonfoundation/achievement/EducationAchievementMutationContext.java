package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;

/**
 * Carries the verified identity and target attributes required before an education-achievement mutation.
 */
public record EducationAchievementMutationContext(
        Long achievementId,
        String evaluationYear,
        String organizationCode,
        String managementItemCode,
        LocalDate occurredDate,
        CurrentUser currentUser,
        String functionType) {
}
