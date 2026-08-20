package kr.ac.knue.commonfoundation.evaluationyears;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationYearSettingsResponse(Integer currentEvaluationYear,
                                             Integer defaultSearchYear,
                                             List<EvaluationYearPreparationRow> preparations,
                                             Long updatedBy,
                                             LocalDateTime updatedAt,
                                             String changeReason) {
}
