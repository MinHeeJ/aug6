package kr.ac.knue.commonfoundation.basic34;

public record CalculationFormulaSearchCriteria(
        int page,
        int pageSize,
        Long ruleVersionId,
        String formulaCode,
        String calculationType,
        String evaluationYear,
        String roundingRule,
        String activeYn,
        String keyword) {
    public int safeSize() {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            return 20;
        }
        return pageSize;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedFormulaCode() { return normalized(formulaCode); }
    public String normalizedCalculationType() { return normalized(calculationType); }
    public String normalizedRoundingRule() { return normalized(roundingRule); }
    public String normalizedKeyword() { return keyword == null || keyword.isBlank() ? null : keyword.trim(); }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
