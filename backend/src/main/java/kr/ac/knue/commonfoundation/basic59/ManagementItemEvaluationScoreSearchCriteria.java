package kr.ac.knue.commonfoundation.basic59;

public record ManagementItemEvaluationScoreSearchCriteria(
        int page,
        int pageSize,
        String achievementAreaCode,
        String achievementCategoryCode,
        String collegeCode) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return safePage() * safeSize();
    }

    public String normalizedAchievementAreaCode() {
        return normalize(achievementAreaCode);
    }

    public String normalizedAchievementCategoryCode() {
        return normalize(achievementCategoryCode);
    }

    public String normalizedCollegeCode() {
        return normalize(collegeCode);
    }

    private String normalize(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
