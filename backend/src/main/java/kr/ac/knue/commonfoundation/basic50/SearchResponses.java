package kr.ac.knue.commonfoundation.basic50;

import java.util.List;

record BusinessSettingSearchResponse(List<BusinessSettingRow> settings, int page, int pageSize, long totalElements) {
}

record AuthoritySearchResponse(List<AuthorityRow> authorities, int page, int pageSize, long totalElements) {
}

record ResearchCriterionSearchResponse(List<ResearchCriterionRow> criteria, int page, int pageSize, long totalElements) {
}

record ResearchAchievementSearchResponse(List<ResearchAchievementRow> achievements, int page, int pageSize, long totalElements) {
}
