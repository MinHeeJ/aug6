package kr.ac.knue.commonfoundation.basic48;

public record ScoreCalculationHistorySearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        Long targetUserId,
        ScoreCalculationHistoryDataScope dataScope,
        String organizationCode,
        Long selfUserId
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

    public String normalizedAreaCode() {
        return normalize(areaCode);
    }

    public String normalizedOrganizationCode() {
        return normalize(organizationCode);
    }

    public boolean organizationScope() {
        return dataScope == ScoreCalculationHistoryDataScope.ORGANIZATION;
    }

    public boolean selfScope() {
        return dataScope == ScoreCalculationHistoryDataScope.SELF;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
