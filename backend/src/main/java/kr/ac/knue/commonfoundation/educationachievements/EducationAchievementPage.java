package kr.ac.knue.commonfoundation.educationachievements;

import java.util.List;

/** Paginated education-achievement response retaining the 20/50/100 list convention. */
public record EducationAchievementPage(List<EducationAchievement> items, int page, int size, long totalElements) {
}
