package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/**
 * Returns non-blocking validation warnings after the mandatory education-achievement write guards have passed.
 */
public record EducationAchievementAccessDecision(List<String> warnings) {
    public boolean hasOccurredDateWarning() {
        return warnings.contains("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD");
    }
}
