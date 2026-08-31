package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;

public record BusinessStatusTransitionRow(
        Long transitionId,
        String definitionVersion,
        String businessType,
        String fromStatusCode,
        String toStatusCode,
        String executorRoleCode,
        String opinionRequiredYn,
        String attachmentRequiredYn,
        String cancellableYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
