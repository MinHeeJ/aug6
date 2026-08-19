package kr.ac.knue.commonfoundation.evaluationyears;

import java.time.LocalDateTime;

public record EvaluationYearPreparationRow(Integer targetYear,
                                           String copyRequestedYn,
                                           String resetRequestedYn,
                                           Long updatedBy,
                                           LocalDateTime updatedAt,
                                           String changeReason) {
}
