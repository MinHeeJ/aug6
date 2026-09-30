package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Search filters for the lecture-evaluation list; null filters are omitted by the mapper. */
public record LectureEvaluationAchievementSearchCriteria(int page, int pageSize, String managementNo,
        String teacherName, String managementItemCode, LocalDate occurredDateFrom, LocalDate occurredDateTo,
        EducationAchievementStatus certificationStatus) { }
