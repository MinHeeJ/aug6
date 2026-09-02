package kr.ac.knue.commonfoundation.basic43;

public record AchievementVerificationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String verificationStatus) {
    public int safeSize() {
        return size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() { return blankToNull(evaluationYear); }
    public String normalizedAreaCode() { return upperBlankToNull(areaCode); }
    public String normalizedVerificationStatus() { return upperBlankToNull(verificationStatus); }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upperBlankToNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
