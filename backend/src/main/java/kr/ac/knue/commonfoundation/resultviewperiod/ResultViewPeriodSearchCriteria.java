package kr.ac.knue.commonfoundation.resultviewperiod;

public record ResultViewPeriodSearchCriteria(
        int page,
        int pageSize,
        String evaluationYear,
        String collegeOrganizationCode,
        String departmentOrganizationCode,
        String visibilityScope,
        String activeYn,
        String keyword,
        Long requesterUserId,
        boolean restrictOrganizationScope
) {
    public ResultViewPeriodSearchCriteria(int page, int pageSize, String evaluationYear,
                                          String collegeOrganizationCode, String departmentOrganizationCode,
                                          String visibilityScope, String activeYn, String keyword) {
        this(page, pageSize, evaluationYear, collegeOrganizationCode, departmentOrganizationCode, visibilityScope,
                activeYn, keyword, null, false);
    }

    public int safeSize() {
        if (pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() { return normalize(evaluationYear); }
    public String normalizedCollegeOrganizationCode() { return normalizeUpper(collegeOrganizationCode); }
    public String normalizedDepartmentOrganizationCode() { return normalizeUpper(departmentOrganizationCode); }
    public String normalizedVisibilityScope() { return normalizeUpper(visibilityScope); }
    public String normalizedActiveYn() { return normalizeUpper(activeYn); }
    public String normalizedKeyword() { return normalize(keyword); }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
