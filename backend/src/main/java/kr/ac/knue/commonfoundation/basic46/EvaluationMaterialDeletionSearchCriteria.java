package kr.ac.knue.commonfoundation.basic46;

public record EvaluationMaterialDeletionSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String generationBatchId) {
    public int safeSize() {
        return switch (size) {
            case 50, 100 -> size;
            default -> 20;
        };
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() {
        return blankToNull(evaluationYear);
    }

    public String normalizedAreaCode() {
        return blankToNull(areaCode);
    }

    public String normalizedGenerationBatchId() {
        return blankToNull(generationBatchId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
