package kr.ac.knue.commonfoundation.basic60;

public record OperationalSettingSearchCriteria(
        int page,
        int pageSize,
        Long ruleVersionId,
        String targetScope,
        String areaCode,
        String itemCode,
        String evaluationYear,
        String elementCode,
        Long managementItemId,
        String managementItemCode,
        String organizationCode,
        Integer researcherCount,
        String participationType,
        String activeYn,
        String keyword) {
    public int safeSize() {
        if (pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedTargetScope() { return upperOrNull(targetScope); }
    public String normalizedAreaCode() { return upperOrNull(areaCode); }
    public String normalizedItemCode() { return upperOrNull(itemCode); }
    public String normalizedElementCode() { return upperOrNull(elementCode); }
    public String normalizedManagementItemCode() { return upperOrNull(managementItemCode); }
    public String normalizedOrganizationCode() { return upperOrNull(organizationCode); }
    public String normalizedParticipationType() { return upperOrNull(participationType); }
    public String normalizedKeyword() { return keyword == null || keyword.isBlank() ? null : keyword.trim(); }

    private String upperOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
