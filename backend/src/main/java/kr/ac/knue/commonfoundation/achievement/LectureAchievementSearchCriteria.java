package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;

/** Search filters for the lecture list; null filters are omitted by the mapper. */
public record LectureAchievementSearchCriteria(int page, int pageSize, String managementNo,
        String teacherName, String managementItemCode, LocalDate occurredDateFrom, LocalDate occurredDateTo,
        EducationAchievementStatus certificationStatus) { }
