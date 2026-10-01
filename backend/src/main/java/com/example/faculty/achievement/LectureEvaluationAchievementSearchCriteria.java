package com.example.faculty.achievement;

import java.time.LocalDate;

/**
 * Represents optional, dynamically composed list filters for lecture-evaluation achievements.
 */
public record LectureEvaluationAchievementSearchCriteria(
        int page,
        int pageSize,
        String managementNo,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDateFrom,
        LocalDate occurredDateTo,
        String certificationStatus,
        Long requesterUserId,
        boolean restrictToRequester
) {
    public int safePage() {
        return Math.max(page, 0);
    }

    public int safePageSize() {
        return pageSize;
    }

    public int offset() {
        return safePage() * safePageSize();
    }
}
