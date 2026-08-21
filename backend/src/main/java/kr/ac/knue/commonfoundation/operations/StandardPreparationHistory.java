package kr.ac.knue.commonfoundation.operations;

import java.time.LocalDateTime;

public record StandardPreparationHistory(Long preparationId, Integer baseYear, String copyRequestedYn, String initializeRequestedYn, String changeReason, LocalDateTime preparedAt, Long preparedBy) {
}
