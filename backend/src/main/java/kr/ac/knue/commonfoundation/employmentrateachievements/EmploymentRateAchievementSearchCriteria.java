package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Pagination and optional safe filters for employment-rate achievement searches. */
public record EmploymentRateAchievementSearchCriteria(
        int page,
        int pageSize,
        String managementNo,
        String managementItemCode,
        String achievementStatus) {
    public int safePage() { return Math.max(page, 0); }
    public int safePageSize() { return pageSize == 50 || pageSize == 100 ? pageSize : 20; }
    public int offset() { return safePage() * safePageSize(); }
    public String normalizedManagementNo() { return normalized(managementNo); }
    public String normalizedManagementItemCode() { return normalized(managementItemCode); }
    public String normalizedAchievementStatus() { return normalized(achievementStatus); }
    private String normalized(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
