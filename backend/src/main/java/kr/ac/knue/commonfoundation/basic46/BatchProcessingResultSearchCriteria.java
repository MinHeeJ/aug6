package kr.ac.knue.commonfoundation.basic46;

import java.util.List;

public record BatchProcessingResultSearchCriteria(
        int page,
        int size,
        String batchType,
        String targetCondition,
        String batchId) {
    public int safeSize() {
        if (size == 50 || size == 100) {
            return size;
        }
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedBatchType() {
        String normalized = normalize(batchType);
        if ("SCORE_RECALCULATION".equals(normalized)) {
            return "RECALCULATION";
        }
        if ("FINALIZATION".equals(normalized) || "FINALIZATION_CANCEL".equals(normalized)) {
            return "CONFIRMATION";
        }
        return normalized;
    }

    public List<String> dbBatchTypes() {
        String normalized = normalizedBatchType();
        if ("RECALCULATION".equals(normalized)) {
            return List.of("SCORE_RECALCULATION");
        }
        if ("CONFIRMATION".equals(normalized)) {
            return List.of("FINALIZATION", "FINALIZATION_CANCEL");
        }
        if (normalized != null) {
            return List.of(normalized);
        }
        return List.of();
    }

    public String normalizedTargetCondition() {
        return normalize(targetCondition);
    }

    public String normalizedBatchId() {
        return normalize(batchId);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
