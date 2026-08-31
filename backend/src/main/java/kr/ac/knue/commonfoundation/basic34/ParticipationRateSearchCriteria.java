package kr.ac.knue.commonfoundation.basic34;

public record ParticipationRateSearchCriteria(
        int page,
        int pageSize,
        Long ruleVersionId,
        Long managementItemId,
        String areaCode,
        String itemCode,
        String evaluationYear,
        String elementCode,
        String managementItemCode,
        Integer researcherCount,
        String participationType,
        String activeYn,
        String keyword) {
    public int safeSize() {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            return 20;
        }
        return pageSize;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedAreaCode() { return normalized(areaCode); }
    public String normalizedItemCode() { return normalized(itemCode); }
    public String normalizedElementCode() { return normalized(elementCode); }
    public String normalizedManagementItemCode() { return normalized(managementItemCode); }
    public String normalizedParticipationType() { return normalized(participationType); }
    public String normalizedKeyword() { return keyword == null || keyword.isBlank() ? null : keyword.trim(); }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
