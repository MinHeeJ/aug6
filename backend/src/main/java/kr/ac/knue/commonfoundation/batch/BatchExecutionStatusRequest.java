package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class BatchExecutionStatusRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("targetStatus", "reason");

    @Size(max = 20, message = "실행상태는 20자 이하여야 합니다.")
    private String targetStatus;
    @NotBlank(message = "처리 사유를 입력하세요.")
    @Size(max = 500, message = "사유는 500자 이하여야 합니다.")
    private String reason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getTargetStatus() { return targetStatus; }
    public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }
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
