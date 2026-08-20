package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDateTime;

public record BaseYearSetting(Integer baseYear, Integer currentEvaluationYear, Integer defaultSearchYear, String copyRequestedYn, String initializeRequestedYn, String changeReason, LocalDateTime updatedAt) {
}
