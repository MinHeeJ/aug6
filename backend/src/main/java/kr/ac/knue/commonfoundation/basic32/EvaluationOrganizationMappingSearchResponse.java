package kr.ac.knue.commonfoundation.basic32;

import java.util.List;

public record EvaluationOrganizationMappingSearchResponse(
        List<EvaluationOrganizationMappingRow> mappings,
        int page,
        int size,
        long totalElements) {
}
