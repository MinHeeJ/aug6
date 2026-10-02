package kr.ac.knue.commonfoundation.basic81;

/** Captures optional degree-completion filters without null-bound SQL predicates. */
public record DegreeCompletionAchievementSearchCriteria(
        int page,
        int pageSize,
        String managementNo,
        String teacherName,
        String certificationStatus) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safePageSize() {
        return pageSize == 50 || pageSize == 100 ? pageSize : 20;
    }

    public int getSafePageSize() {
        return safePageSize();
    }

    public int offset() {
        return safePage() * safePageSize();
    }

    public int getOffset() {
        return offset();
    }

    public String normalizedManagementNo() {
        return trimToNull(managementNo);
    }

    public String normalizedTeacherName() {
        return trimToNull(teacherName);
    }

    public String normalizedCertificationStatus() {
        return trimToNull(certificationStatus);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
