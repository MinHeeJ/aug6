package kr.ac.knue.commonfoundation.basic36;

public record FullTimeFacultyStatusSearchCriteria(int page, int pageSize, Integer baseYear,
                                                  String organizationCode, String employeeNo, String name) {
    public int safeSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedOrganizationCode() {
        return normalizeUpper(organizationCode);
    }

    public String normalizedEmployeeNo() {
        return normalizeUpper(employeeNo);
    }

    public String normalizedName() {
        return hasText(name) ? name.trim() : null;
    }

    private String normalizeUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
