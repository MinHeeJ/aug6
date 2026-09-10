package kr.ac.knue.commonfoundation.basic59;

import java.util.List;

public record ManagementItemEvaluationScoreSearchResponse(
        List<ManagementItemEvaluationScoreRow> managementItemEvaluationScores,
        int page,
        int pageSize,
        long totalElements) {
}
