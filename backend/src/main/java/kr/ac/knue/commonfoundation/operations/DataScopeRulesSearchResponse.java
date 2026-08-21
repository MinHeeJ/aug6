package kr.ac.knue.commonfoundation.operations;

import java.util.List;

public record DataScopeRulesSearchResponse(
        List<DataScopeRuleRow> rules,
        int page,
        int size,
        int totalElements
) {
}
