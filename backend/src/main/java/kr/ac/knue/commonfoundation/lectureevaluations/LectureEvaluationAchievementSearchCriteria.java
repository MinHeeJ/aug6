package kr.ac.knue.commonfoundation.lectureevaluations;

import java.time.LocalDate;

/** Optional, normalized search predicates for lecture-evaluation achievements. */
public record LectureEvaluationAchievementSearchCriteria(String managementNo, String teacherName,
        String managementItemCode, LocalDate occurredDateFrom, LocalDate occurredDateTo, String certificationStatus) {
}
