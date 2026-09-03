package kr.ac.knue.commonfoundation.basic46;

public record FinalEvaluationConfirmationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        Long targetUserId,
        String finalStatus) {
    public int safeSize() {
        return switch (size) {
            case 50 -> 50;
            case 100 -> 100;
            default -> 20;
        };
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedEvaluationYear() {
        return evaluationYear == null || evaluationYear.trim().isBlank() ? null : evaluationYear.trim();
    }

    public String normalizedFinalStatus() {
        return finalStatus == null || finalStatus.trim().isBlank() ? null : finalStatus.trim();
    }
}
