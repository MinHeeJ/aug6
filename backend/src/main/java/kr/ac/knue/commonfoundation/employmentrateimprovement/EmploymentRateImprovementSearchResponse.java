package kr.ac.knue.commonfoundation.employmentrateimprovement;

import java.util.List;

/** Paged list response for 취업률 제고 실적 using the approved page-size choices. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}
