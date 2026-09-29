package kr.ac.knue.commonfoundation.studentguidance;

import java.util.List;

/** Paged response used by the student-guidance list route. */
public record StudentGuidanceAchievementSearchResponse(List<StudentGuidanceAchievementRow> achievements,
        int page, int pageSize, long totalElements) { }
