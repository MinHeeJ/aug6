package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;

public record RejectionReasonRow(
        Long rejectionReasonId,
        String businessType,
        String reasonCode,
        String standardMessage,
        String additionalOpinionAllowedYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
