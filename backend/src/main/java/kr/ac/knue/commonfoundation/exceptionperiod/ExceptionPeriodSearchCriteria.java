package kr.ac.knue.commonfoundation.exceptionperiod;

public record ExceptionPeriodSearchCriteria(
        int page,
        int pageSize,
        String evaluationYear,
        Long teacherUserId,
        String areaCode,
        String targetFunctionCode,
        String activeYn,
        String keyword
) {
    public int safeSize() {
        if (pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() { return normalize(evaluationYear); }
    public String normalizedAreaCode() { return normalizeUpper(areaCode); }
    public String normalizedTargetFunctionCode() { return normalizeUpper(targetFunctionCode); }
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
