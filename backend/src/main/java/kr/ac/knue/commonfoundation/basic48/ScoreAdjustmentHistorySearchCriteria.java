package kr.ac.knue.commonfoundation.basic48;

public record ScoreAdjustmentHistorySearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        Long targetUserId,
        String adjustmentTarget,
        ScoreAdjustmentHistoryDataScope dataScope,
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

    public String normalizedAreaCode() {
        return normalize(areaCode);
    }

    public String normalizedAdjustmentTarget() {
        return normalize(adjustmentTarget);
    }

    public String normalizedOrganizationCode() {
        return normalize(organizationCode);
    }

    public boolean organizationScope() {
        return dataScope == ScoreAdjustmentHistoryDataScope.ORGANIZATION;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
