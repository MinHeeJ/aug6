package kr.ac.knue.commonfoundation.basic45;

public record ScoreRecalculationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String targetUserId,
        String formulaVersionId) {
    public int safeSize() {
        if (size == 50 || size == 100) {
            return size;
        }
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() {
        return normalize(evaluationYear);
    }

    public String normalizedAreaCode() {
        return normalize(areaCode);
    }

    public Long normalizedTargetUserId() {
        String value = normalize(targetUserId);
        return value == null ? null : Long.valueOf(value);
    }

    public Long normalizedFormulaVersionId() {
        String value = normalize(formulaVersionId);
        return value == null ? null : Long.valueOf(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
