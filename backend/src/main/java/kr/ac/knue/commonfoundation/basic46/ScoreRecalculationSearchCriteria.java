package kr.ac.knue.commonfoundation.basic46;

public record ScoreRecalculationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        Long targetUserId) {
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
