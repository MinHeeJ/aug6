package kr.ac.knue.commonfoundation.f3_user_story_2;

/** Captures optional list filters and exposes safe pagination values to the persistence adapter. */
public record CourseOperationSearchCriteria(
        int page,
        int pageSize,
        String managementNo,
        String teacherName,
        String managementItemCode,
        String certificationStatus) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safePageSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int offset() {
        return safePage() * safePageSize();
    }

    public String normalizedManagementNo() {
        return trimToNull(managementNo);
    }

    public String normalizedTeacherName() {
        return trimToNull(teacherName);
    }

    public String normalizedManagementItemCode() {
        return trimToNull(managementItemCode);
    }

    public String normalizedCertificationStatus() {
        return trimToNull(certificationStatus);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
