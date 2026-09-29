package kr.ac.knue.commonfoundation.faculty.achievement;

import java.util.List;

public record DegreeCompletionSearchResponse(List<DegreeCompletionAchievementRow> items, int page, int pageSize, long totalElements) { }