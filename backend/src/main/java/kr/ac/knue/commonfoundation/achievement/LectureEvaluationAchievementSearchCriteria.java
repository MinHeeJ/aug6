package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/**
 * Normalized optional filters and bounded paging for the lecture-evaluation achievement list.
 */
public record LectureEvaluationAchievementSearchCriteria(
        int page,
        int size,
        String managementNo,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDateFrom,
        LocalDate occurredDateTo,
        String certificationStatus,
        Long viewerUserId,
        String viewerRole
) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        return size == 50 || size == 100 ? size : 20;
    }

    public int offset() {
        return safePage() * safeSize();
    }
}
