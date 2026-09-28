package kr.ac.knue.commonfoundation.educationachievement;

import java.util.List;

/** Wraps a paged education-achievement list using the existing API response envelope. */
public record EducationAchievementSearchResponse(List<EducationAchievementRow> items, int page, int size, long totalElements) {
}
