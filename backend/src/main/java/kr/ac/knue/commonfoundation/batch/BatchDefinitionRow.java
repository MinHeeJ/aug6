package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record BatchDefinitionRow(
        String batchId,
        String batchType,
        String scheduleCycle,
        Integer maxExecutionSeconds,
        Long ownerUserId,
        String ownerName,
        String requestId,
        LocalDateTime updatedAt,
        Long updatedBy,
        List<String> predecessorBatchIds,
        List<String> successorBatchIds,
        JsonNode parameters,
        @JsonIgnore String parameterJson) {
    public BatchDefinitionRow(String batchId, String batchType, String scheduleCycle, Integer maxExecutionSeconds,
            Long ownerUserId, String ownerName, String requestId, LocalDateTime updatedAt, Long updatedBy,
            String parameterJson) {
        this(batchId, batchType, scheduleCycle, maxExecutionSeconds, ownerUserId, ownerName, requestId, updatedAt,
                updatedBy, Collections.emptyList(), Collections.emptyList(), null, parameterJson);
    }

    public BatchDefinitionRow withChildren(List<String> predecessors, List<String> successors, JsonNode parsedParameters) {
        return new BatchDefinitionRow(batchId, batchType, scheduleCycle, maxExecutionSeconds, ownerUserId, ownerName,
                requestId, updatedAt, updatedBy, predecessors, successors, parsedParameters, parameterJson);
    }
}
