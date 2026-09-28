package kr.ac.knue.commonfoundation.educationachievement;

import java.util.List;

/**
 * Standard paged envelope for education-area achievement lists.
 */
public record EducationAchievementSearchResponse(
        List<EducationAchievementRow> items,
        int page,
        int size,
        long totalElements) {
}
