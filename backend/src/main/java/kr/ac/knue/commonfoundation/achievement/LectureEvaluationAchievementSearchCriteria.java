package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Search filters for the lecture-evaluation list; absent filters are omitted by the mapper. */
public record LectureEvaluationAchievementSearchCriteria(
        String evaluationYear,
        String managementNo,
        String teacherName,
        String managementItemCode,
        LocalDate occurredDateFrom,
        LocalDate occurredDateTo,
        String certificationStatus,
        int limit,
        int offset) {
}
