package kr.ac.knue.commonfoundation.basic36;

public record ResearcherProfileSearchCriteria(int page, int size, String employeeNo, String name, String organizationCode,
                                              String requesterEmployeeNo, boolean selfOnly) {
    public int safeSize() { return size == 50 || size == 100 ? size : 20; }
    public int offset() { return Math.max(page, 0) * safeSize(); }
    public String normalizedEmployeeNo() { return normalizeUpper(employeeNo); }
    public String normalizedOrganizationCode() { return normalizeUpper(organizationCode); }
    public String normalizedName() { return hasText(name) ? name.trim() : null; }
    public String normalizedRequesterEmployeeNo() { return normalizeUpper(requesterEmployeeNo); }
    private String normalizeUpper(String value) { return hasText(value) ? value.trim().toUpperCase() : null; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
