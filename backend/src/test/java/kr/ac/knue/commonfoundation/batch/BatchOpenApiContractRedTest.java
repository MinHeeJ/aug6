package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BatchOpenApiContractRedTest {
    private final String openApi;

    BatchOpenApiContractRedTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void batchDefinitionOperationsAreDeclaredWithSessionCookieAndR09Role() {
        assertOperationContract("/api/admin/batch-definitions:", "get:", "operationId: listBatchDefinitions",
                "page", "size", "batchId", "batchType", "scheduleCycle", "REQ-409");
        assertOperationContract("/api/admin/batch-definitions:", "post:", "operationId: saveBatchDefinition",
                "batchId", "scheduleCycle", "ownerUserId", "predecessorBatchIds", "successorBatchIds", "parameters", "REQ-410");
    }

    @Test
    void batchExecutionOperationsAreDeclaredWithRequiredReasonsAndOriginalExecutionLink() {
        assertOperationContract("/api/admin/batch-executions:", "get:", "operationId: listBatchExecutions",
                "page", "size", "batchId", "executionStatus", "REQ-426");
        assertOperationContract("/api/admin/batch-executions:", "post:", "operationId: createBatchExecution",
                "batchId", "reason", "operatorUserId", "MANUAL_RUN", "REQ-427");
        assertOperationContract("/api/admin/batch-executions/{executionId}/status:", "patch:", "operationId: updateBatchExecutionStatus",
                "executionId", "targetStatus", "reason", "STOPPED", "REQ-428");
        assertOperationContract("/api/admin/batch-executions/{executionId}/rerun:", "post:", "operationId: createBatchRerun",
                "executionId", "originalExecutionId", "reason", "RERUN", "REQ-429");
    }

    @Test
    void batchResultOperationsAreReadOnlyAndExposeResultCountsAndLogReference() {
        assertOperationContract("/api/admin/batch-results:", "get:", "operationId: listBatchResults",
                "startedAt", "endedAt", "totalCount", "successCount", "failureCount", "excludedCount", "elapsedMillis", "REQ-449");
        assertOperationContract("/api/admin/batch-results/{executionId}/log:", "get:", "operationId: getBatchResultLog",
                "executionId", "logFileRef", "REQ-457");
        assertThat(openApi).doesNotContain("operationId: updateBatchResult", "operationId: deleteBatchResultLog");
    }

    @Test
    void batchRetryOperationsRequireFailedTargetsAndRetryReason() {
        assertOperationContract("/api/admin/batch-retries/targets:", "get:", "operationId: listBatchRetryTargets",
                "originalExecutionId", "failedItemKey", "FAILED", "REQ-466");
        assertOperationContract("/api/admin/batch-retries:", "post:", "operationId: createBatchRetry",
                "originalExecutionId", "failedItemKey", "retryReason", "retryExecutionId", "REQ-468", "REQ-480");
    }

    private void assertOperationContract(String path, String method, String operationId, String... requiredSnippets) {
        int pathIndex = openApi.indexOf(path);
        assertThat(pathIndex).as(path + " path must exist in durable OpenAPI fixture").isGreaterThanOrEqualTo(0);
        int methodIndex = openApi.indexOf(method, pathIndex);
        assertThat(methodIndex).as(method + " method must exist after " + path).isGreaterThanOrEqualTo(pathIndex);
        String operationBlock = openApi.substring(methodIndex, Math.min(openApi.length(), methodIndex + 4500));
        assertThat(operationBlock)
                .contains(operationId, "'200':", "'400':", "'401':", "'403':", "security:", "SessionCookie", "x-roles", "R09")
                .contains(requiredSnippets);
    }
}
