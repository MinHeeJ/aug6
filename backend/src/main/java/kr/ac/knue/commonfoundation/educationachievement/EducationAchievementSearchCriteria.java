package kr.ac.knue.commonfoundation.educationachievement;

import java.util.Set;

/** Normalizes list paging and restricts the endpoint to education-achievement types from the API contract. */
public record EducationAchievementSearchCriteria(String achievementType, int page, int size) {
    private static final Set<String> TYPES = Set.of(
            "LECTURE_EVALUATION", "LECTURE_ACHIEVEMENT", "STUDENT_GUIDANCE", "DEGREE_COMPLETION");

    public String normalizedAchievementType() {
        return achievementType == null ? null : achievementType.trim().toUpperCase();
    }

    public boolean hasSupportedAchievementType() {
        return TYPES.contains(normalizedAchievementType());
    }

    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return size == 20 || size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return safePage() * safeSize();
    }
}
