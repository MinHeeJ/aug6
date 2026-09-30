package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/**
 * Holds optional, dynamically applied list filters for lecture-evaluation achievements.
 */
public record LectureEvaluationAchievementSearchCriteria(
        int page,
        int size,
        String managementNo,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDateFrom,
        LocalDate occurredDateTo,
        String certificationStatus) {

    public int offset() {
        return Math.max(page, 0) * size;
    }
}
