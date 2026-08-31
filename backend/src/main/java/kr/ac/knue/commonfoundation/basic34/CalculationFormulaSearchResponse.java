package kr.ac.knue.commonfoundation.basic34;

import java.util.List;

public record CalculationFormulaSearchResponse(
        List<CalculationFormulaRow> calculationFormulas,
        int page,
        int pageSize,
        long totalElements) {
}
