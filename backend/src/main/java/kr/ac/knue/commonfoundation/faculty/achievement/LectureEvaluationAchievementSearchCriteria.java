package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;

/** Query filters and safe paging values for lecture-evaluation achievement lists. */
public record LectureEvaluationAchievementSearchCriteria(
        int page,
        int size,
        String managementNo,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDateFrom,
        LocalDate occurredDateTo,
        String certificationStatus
) {
    public int safeSize() {
        return size == 20 || size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return Math.max(page, 0) * safeSize();
    }

    public String normalizedManagementNo() {
        return normalized(managementNo);
    }

    public String normalizedTeacherName() {
        return normalized(teacherName);
    }

    public String normalizedManagementItemCode() {
        return normalized(managementItemCode);
    }

    public String normalizedCertificationStatus() {
        return normalized(certificationStatus);
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
