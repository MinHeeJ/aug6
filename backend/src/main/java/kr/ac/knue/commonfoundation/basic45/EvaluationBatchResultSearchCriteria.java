package kr.ac.knue.commonfoundation.basic45;

public record EvaluationBatchResultSearchCriteria(
        int page,
        int size,
        String batchId,
        String jobType,
        String targetCondition) {
    public int safeSize() {
        if (size == 50 || size == 100) {
            return size;
        }
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedBatchId() {
        return blankToNull(batchId);
    }

    public String normalizedJobType() {
        return blankToNull(jobType);
    }

    public String normalizedTargetCondition() {
        return blankToNull(targetCondition);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
