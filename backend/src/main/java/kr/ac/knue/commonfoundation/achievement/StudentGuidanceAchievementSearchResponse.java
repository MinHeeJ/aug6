package kr.ac.knue.commonfoundation.achievement;

import java.util.List;

/** Paged student-guidance response used by the protected business list endpoint. */
public record StudentGuidanceAchievementSearchResponse(
        List<StudentGuidanceAchievementRow> achievements, int page, int size, long totalElements
) {
}
