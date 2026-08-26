package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record BatchExecutionRow(
        String executionId,
        String batchId,
        String batchType,
        String executionStatus,
        String processType,
        String reason,
        Long operatorUserId,
        String operatorName,
        String originalExecutionId,
        String requestId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        JsonNode parameters,
        @JsonIgnore String executionParameterJson) {
    public BatchExecutionRow(String executionId, String batchId, String batchType, String executionStatus,
            String processType, String reason, Long operatorUserId, String operatorName, String originalExecutionId,
            String requestId, LocalDateTime createdAt, LocalDateTime updatedAt, String executionParameterJson) {
        this(executionId, batchId, batchType, executionStatus, processType, reason, operatorUserId, operatorName,
                originalExecutionId, requestId, createdAt, updatedAt, null, executionParameterJson);
    }

    public BatchExecutionRow withParameters(JsonNode parsedParameters) {
        return new BatchExecutionRow(executionId, batchId, batchType, executionStatus, processType, reason,
                operatorUserId, operatorName, originalExecutionId, requestId, createdAt, updatedAt, parsedParameters,
                executionParameterJson);
    }
}
