package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;

public record BusinessStatusCodeRow(
        Long statusCodeId,
        String definitionVersion,
        String businessType,
        String statusCode,
        String displayName,
        String systemUseYn,
        String changeReason,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
