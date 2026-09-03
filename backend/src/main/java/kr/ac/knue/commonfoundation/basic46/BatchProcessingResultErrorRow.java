package kr.ac.knue.commonfoundation.basic46;

import java.time.LocalDateTime;

public record BatchProcessingResultErrorRow(
        Long batchJobItemId,
        String batchId,
        String targetRef,
        String resultStatus,
        String errorCode,
        String errorMessage,
        String excludedReason,
        LocalDateTime processedAt) {
}
