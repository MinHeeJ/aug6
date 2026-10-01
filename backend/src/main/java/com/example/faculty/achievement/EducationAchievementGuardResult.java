package com.example.faculty.achievement;

/**
 * Communicates whether the guard accepted a mutation normally or accepted it with the required
 * occurrence-date warning.
 */
public record EducationAchievementGuardResult(boolean warning, String warningCode) {
    public static EducationAchievementGuardResult accepted() {
        return new EducationAchievementGuardResult(false, null);
    }

    public static EducationAchievementGuardResult warning(String warningCode) {
        return new EducationAchievementGuardResult(true, warningCode);
    }
}
