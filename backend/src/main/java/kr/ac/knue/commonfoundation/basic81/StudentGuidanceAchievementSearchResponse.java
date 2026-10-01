package kr.ac.knue.commonfoundation.basic81;

import java.util.List;

/** Paged response for the student-guidance list. */
public record StudentGuidanceAchievementSearchResponse(
        List<StudentGuidanceAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
