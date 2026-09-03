package kr.ac.knue.commonfoundation.basic45;

public record FinalEvaluationConfirmationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String targetUserId,
        String confirmationStatus) {
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
        return blankToNull(evaluationYear);
    }

    public String normalizedAreaCode() {
        return blankToNull(areaCode);
    }

    public Long normalizedTargetUserId() {
        String normalized = blankToNull(targetUserId);
        return normalized == null ? null : Long.valueOf(normalized);
    }

    public String normalizedConfirmationStatus() {
        return blankToNull(confirmationStatus);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
