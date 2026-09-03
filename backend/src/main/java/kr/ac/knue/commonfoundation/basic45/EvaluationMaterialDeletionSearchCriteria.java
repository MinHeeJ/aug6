package kr.ac.knue.commonfoundation.basic45;

public record EvaluationMaterialDeletionSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String generationBatchId) {
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

    public String normalizedGenerationBatchId() {
        return normalize(generationBatchId);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
