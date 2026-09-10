package kr.ac.knue.commonfoundation.basic59;

public record EvaluationElementManagementItemSearchCriteria(
        int page,
        int pageSize,
        String evaluationYear,
        String areaCode,
        String elementCode) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return safePage() * safeSize();
    }

    public String normalizedAreaCode() {
        return normalize(areaCode);
    }

    public String normalizedElementCode() {
        return normalize(elementCode);
    }

    public String normalizedEvaluationYear() {
        String trimmed = trim(evaluationYear);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private String normalize(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed.toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
