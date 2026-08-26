package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BatchDefinitionRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("batchId", "batchType", "scheduleCycle",
            "predecessorBatchIds", "successorBatchIds", "parameters", "maxExecutionSeconds", "ownerUserId");

    @NotBlank(message = "배치ID를 입력하세요.")
    @Size(max = 100, message = "배치ID는 100자 이하여야 합니다.")
    private String batchId;

    @NotBlank(message = "업무유형을 입력하세요.")
    @Size(max = 50, message = "업무유형은 50자 이하여야 합니다.")
    private String batchType;

    @NotBlank(message = "실행주기를 입력하세요.")
    @Size(max = 100, message = "실행주기는 100자 이하여야 합니다.")
    private String scheduleCycle;

    private List<String> predecessorBatchIds = new ArrayList<>();
    private List<String> successorBatchIds = new ArrayList<>();
    private JsonNode parameters;
    private Integer maxExecutionSeconds;

    @NotNull(message = "담당자를 입력하세요.")
    private Long ownerUserId;

    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getBatchType() { return batchType; }
    public void setBatchType(String batchType) { this.batchType = batchType; }
    public String getScheduleCycle() { return scheduleCycle; }
    public void setScheduleCycle(String scheduleCycle) { this.scheduleCycle = scheduleCycle; }
    public List<String> getPredecessorBatchIds() { return predecessorBatchIds == null ? Collections.emptyList() : predecessorBatchIds; }
    public void setPredecessorBatchIds(List<String> predecessorBatchIds) { this.predecessorBatchIds = predecessorBatchIds; }
    public List<String> getSuccessorBatchIds() { return successorBatchIds == null ? Collections.emptyList() : successorBatchIds; }
    public void setSuccessorBatchIds(List<String> successorBatchIds) { this.successorBatchIds = successorBatchIds; }
    public JsonNode getParameters() { return parameters; }
    public void setParameters(JsonNode parameters) { this.parameters = parameters; }
    public Integer getMaxExecutionSeconds() { return maxExecutionSeconds; }
    public void setMaxExecutionSeconds(Integer maxExecutionSeconds) { this.maxExecutionSeconds = maxExecutionSeconds; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Set<String> getUnexpectedFields() { return unexpectedFields; }

    @JsonAnySetter
    public void captureUnexpectedField(String field, Object ignored) {
        if (!ALLOWED_FIELDS.contains(field)) {
            unexpectedFields.add(field);
        }
    }
}
