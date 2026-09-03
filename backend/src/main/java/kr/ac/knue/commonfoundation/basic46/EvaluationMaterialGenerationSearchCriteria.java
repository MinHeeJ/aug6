package kr.ac.knue.commonfoundation.basic46;

public record EvaluationMaterialGenerationSearchCriteria(
        int page,
        int size,
        String evaluationYear,
        String areaCode,
        String organizationCode,
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

    public String normalizedOrganizationCode() {
        return blankToNull(organizationCode);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
