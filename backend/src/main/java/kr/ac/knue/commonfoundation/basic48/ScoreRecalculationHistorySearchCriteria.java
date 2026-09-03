package kr.ac.knue.commonfoundation.basic48;

public record ScoreRecalculationHistorySearchCriteria(
        int page,
        int size,
        String evaluationYear,
        Long targetUserId,
        String executedFrom,
        String executedTo,
        ScoreRecalculationHistoryDataScope dataScope,
        String organizationCode
) {
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
        return normalize(evaluationYear);
    }

    public String normalizedExecutedFrom() {
        return normalize(executedFrom);
    }

    public String normalizedExecutedTo() {
        return normalize(executedTo);
    }

    public String normalizedOrganizationCode() {
        return normalize(organizationCode);
    }

    public boolean organizationScope() {
        return dataScope == ScoreRecalculationHistoryDataScope.ORGANIZATION;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
