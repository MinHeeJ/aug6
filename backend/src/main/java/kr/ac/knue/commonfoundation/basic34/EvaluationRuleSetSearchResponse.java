package kr.ac.knue.commonfoundation.basic34;

import java.util.List;

public record EvaluationRuleSetSearchResponse(
        List<EvaluationRuleSetRow> evaluationRuleSets,
        int page,
        int pageSize,
        long totalElements
) {}
