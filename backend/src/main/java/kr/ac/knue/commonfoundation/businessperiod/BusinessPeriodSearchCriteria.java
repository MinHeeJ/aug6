package kr.ac.knue.commonfoundation.businessperiod;

public record BusinessPeriodSearchCriteria(
        int page,
        int pageSize,
        String evaluationYear,
        String areaCode,
        String organizationCode,
        String userTypeCode,
        String activeYn,
        String keyword,
        Long requesterUserId,
        boolean restrictOrganizationScope
) {
    public BusinessPeriodSearchCriteria(int page, int pageSize, String evaluationYear, String areaCode,
                                        String organizationCode, String userTypeCode, String activeYn,
                                        String keyword) {
        this(page, pageSize, evaluationYear, areaCode, organizationCode, userTypeCode, activeYn, keyword, null, false);
    }

    public int safeSize() {
        if (pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() { return normalize(evaluationYear); }
    public String normalizedAreaCode() { return normalizeUpper(areaCode); }
    public String normalizedOrganizationCode() { return normalizeUpper(organizationCode); }
    public String normalizedUserTypeCode() { return normalizeUpper(userTypeCode); }
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
