package kr.ac.knue.commonfoundation.evaluationyears;

import java.time.LocalDateTime;

public record EvaluationYearSettingsRow(Integer currentEvaluationYear,
                                        Integer defaultSearchYear,
                                        Long updatedBy,
                                        LocalDateTime updatedAt,
                                        String changeReason) {
}
