package kr.ac.knue.commonfoundation.basic34;

public record EvaluationRuleSetSearchCriteria(
        int page,
        int pageSize,
        Long ruleVersionId,
        String targetScope,
        String ruleSetName,
        String ruleSetStatus,
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

    public String normalizedTargetScope() {
        return normalize(targetScope);
    }

    public String normalizedRuleSetName() {
        return normalize(ruleSetName);
    }

    public String normalizedRuleSetStatus() {
        return normalize(ruleSetStatus);
    }

    public String normalizedActiveYn() {
        return normalize(activeYn);
    }

    public String normalizedKeyword() {
        return normalize(keyword);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
