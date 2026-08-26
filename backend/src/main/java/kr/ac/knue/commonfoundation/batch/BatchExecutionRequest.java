package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class BatchExecutionRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("batchId", "parameters", "reason");

    @Size(max = 100, message = "배치ID는 100자 이하여야 합니다.")
    private String batchId;
    private JsonNode parameters;
    @NotBlank(message = "처리 사유를 입력하세요.")
    @Size(max = 500, message = "사유는 500자 이하여야 합니다.")
    private String reason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public JsonNode getParameters() { return parameters; }
    public void setParameters(JsonNode parameters) { this.parameters = parameters; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Set<String> getUnexpectedFields() { return unexpectedFields; }

    @JsonAnySetter
    public void captureUnexpectedField(String field, Object ignored) {
        if (!ALLOWED_FIELDS.contains(field)) {
            unexpectedFields.add(field);
        }
    }
}
