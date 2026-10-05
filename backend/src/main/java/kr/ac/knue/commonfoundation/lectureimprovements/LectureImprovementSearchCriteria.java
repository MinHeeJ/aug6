package kr.ac.knue.commonfoundation.lectureimprovements;

/** Captures optional list filters without binding null predicates in PostgreSQL. */
public record LectureImprovementSearchCriteria(
        int page,
        int pageSize,
        String managementNo,
        String managementItemCode,
        Integer academicYear,
        Integer semester,
        String achievementStatus) {
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

    public String normalizedManagementItemCode() {
        return trimToNull(managementItemCode);
    }

    public String normalizedAchievementStatus() {
        return trimToNull(achievementStatus);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
