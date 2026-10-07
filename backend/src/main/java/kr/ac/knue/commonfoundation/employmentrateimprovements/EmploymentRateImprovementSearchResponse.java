package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;

/** Existing education list envelope, with caller-scoped count and rows. */
public record EmploymentRateImprovementSearchResponse(
        List<EmploymentRateImprovementRow> achievements, int page, int pageSize, long totalElements) {
}
