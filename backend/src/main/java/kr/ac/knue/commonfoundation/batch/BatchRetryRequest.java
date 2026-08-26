package kr.ac.knue.commonfoundation.batch;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class BatchRetryRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("originalExecutionId", "failedItemKey", "retryReason");

    @NotBlank(message = "원실행ID를 선택하세요.")
    @Size(max = 100, message = "원실행ID는 100자 이하여야 합니다.")
    private String originalExecutionId;

    @Size(max = 200, message = "실패 건 식별자는 200자 이하여야 합니다.")
    private String failedItemKey;

    @NotBlank(message = "재처리 사유를 입력하세요.")
    @Size(max = 500, message = "재처리 사유는 500자 이하여야 합니다.")
    private String retryReason;

    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getOriginalExecutionId() { return originalExecutionId; }
    public void setOriginalExecutionId(String originalExecutionId) { this.originalExecutionId = originalExecutionId; }
    public String getFailedItemKey() { return failedItemKey; }
    public void setFailedItemKey(String failedItemKey) { this.failedItemKey = failedItemKey; }
    public String getRetryReason() { return retryReason; }
    public void setRetryReason(String retryReason) { this.retryReason = retryReason; }
    public Set<String> getUnexpectedFields() { return unexpectedFields; }

    @JsonAnySetter
    public void captureUnexpectedField(String field, Object ignored) {
        if (!ALLOWED_FIELDS.contains(field)) {
            unexpectedFields.add(field);
        }
    }
}
