package kr.ac.knue.commonfoundation.basic29;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic29FoundationContractRedTest {
    private final String openApi;

    Basic29FoundationContractRedTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void basic29AdminOperationsRequireSessionCookieAndR09OnlyAuthorization() {
        for (OperationContract operation : operations()) {
            String block = operationBlock(operation);
            assertThat(block)
                    .as(operation.operationId() + " must preserve the common R09 administrator boundary")
                    .contains("security:", "SessionCookie", "x-roles:", "R09")
                    .doesNotContain("- R01", "- R02", "- R03", "- R04", "- R05", "- R06", "- R07", "- R08");
        }
    }

    @Test
    void basic29OperationsDeclareApiEnvelopeAndSensitiveSafeErrorResponses() {
        for (OperationContract operation : operations()) {
            String block = operationBlock(operation);
            assertThat(block)
                    .as(operation.operationId() + " must use existing ApiResponse/ApiError envelopes")
                    .contains("'200':", "'400':", "'401':", "'403':", "ApiResponse", "ApiError")
                    .doesNotContain("password_hash", "resident_registration_number", "account_number_plain");
        }
    }

    @Test
    void basic29OperationsPropagateRequestIdThroughHeaderResponseMetaAndAuditSideEffects() {
        for (OperationContract operation : operations()) {
            String block = operationBlock(operation);
            assertThat(block)
                    .as(operation.operationId() + " must declare end-to-end request id propagation")
                    .contains("X-Request-Id", "requestId", "x-side-effects", "REQ-661");
        }
        assertThat(operationBlock(new OperationContract("/api/admin/security/active-sessions/{sessionId}/terminate:", "post:", "operationId: terminateActiveSession")))
                .contains("business_process_audit_logs", "SESSION_TERMINATE", "request_id");
    }

    @Test
    void basic29ListOperationsDeclareDefaultTwentyAndAllowedPageSizesWithSafeFilters() {
        List<OperationContract> listOperations = List.of(
                new OperationContract("/api/admin/security/active-sessions:", "get:", "operationId: listActiveSessions"),
                new OperationContract("/api/admin/security/session-termination-histories:", "get:", "operationId: listSessionTerminationHistories"),
                new OperationContract("/api/admin/audit/business-process-logs:", "get:", "operationId: listBusinessProcessLogs"),
                new OperationContract("/api/admin/audit/sensitive-information-access-logs:", "get:", "operationId: listSensitiveInformationAccessLogs"),
                new OperationContract("/api/admin/audit/permission-change-logs:", "get:", "operationId: listPermissionChangeLogs"));

        for (OperationContract operation : listOperations) {
            String block = operationBlock(operation);
            assertThat(block)
                    .as(operation.operationId() + " must share the common 20/50/100 pagination and dynamic filter contract")
                    .contains("page", "pageSize", "default: 20", "enum:", "20", "50", "100")
                    .contains("filter", "x-business-rules", "동적 predicate")
                    .doesNotContain("is null or", "coalesce(:", "coalesce(#{", "? is null");
        }
    }

    @Test
    void basic29OperationsCarryRequiredRequirementAccountingAndVendorObligations() {
        assertOperationContract("/api/admin/security/active-sessions:", "get:", "operationId: listActiveSessions",
                "REQ-644", "REQ-645", "REQ-679", "REQ-680", "sessions", "ACTIVE");
        assertOperationContract("/api/admin/security/active-sessions/{sessionId}/terminate:", "post:", "operationId: terminateActiveSession",
                "REQ-661", "REQ-662", "REQ-681", "REQ-682", "REQ-683", "REQ-684", "x-required-tests", "x-state-transitions", "ACTIVE -> TERMINATED", "reason");
        assertOperationContract("/api/admin/security/session-termination-histories:", "get:", "operationId: listSessionTerminationHistories",
                "REQ-687", "REQ-688", "REQ-689", "REQ-690", "REQ-692", "REQ-693", "session_termination_history");
        assertOperationContract("/api/admin/audit/business-process-logs:", "get:", "operationId: listBusinessProcessLogs",
                "REQ-694", "REQ-695", "REQ-696", "REQ-697", "REQ-698", "REQ-699", "business_process_audit_logs");
        assertOperationContract("/api/admin/audit/sensitive-information-access-logs:", "get:", "operationId: listSensitiveInformationAccessLogs",
                "REQ-701", "REQ-702", "REQ-703", "REQ-704", "REQ-705", "REQ-706", "sensitive_information_access_logs");
        assertOperationContract("/api/admin/audit/permission-change-logs:", "get:", "operationId: listPermissionChangeLogs",
                "REQ-707", "REQ-708", "REQ-709", "REQ-711", "REQ-712", "permission_change_history", "approverUserId");
    }

    private void assertOperationContract(String path, String method, String operationId, String... requiredSnippets) {
        assertThat(operationBlock(new OperationContract(path, method, operationId))).contains(requiredSnippets);
    }

    private List<OperationContract> operations() {
        return List.of(
                new OperationContract("/api/admin/security/active-sessions:", "get:", "operationId: listActiveSessions"),
                new OperationContract("/api/admin/security/active-sessions/{sessionId}/terminate:", "post:", "operationId: terminateActiveSession"),
                new OperationContract("/api/admin/security/session-termination-histories:", "get:", "operationId: listSessionTerminationHistories"),
                new OperationContract("/api/admin/audit/business-process-logs:", "get:", "operationId: listBusinessProcessLogs"),
                new OperationContract("/api/admin/audit/sensitive-information-access-logs:", "get:", "operationId: listSensitiveInformationAccessLogs"),
                new OperationContract("/api/admin/audit/permission-change-logs:", "get:", "operationId: listPermissionChangeLogs"));
    }

    private String operationBlock(OperationContract operation) {
        int pathIndex = openApi.indexOf("  " + operation.path());
        assertThat(pathIndex).as(operation.path() + " path must exist in durable OpenAPI fixture").isGreaterThanOrEqualTo(0);
        int methodIndex = openApi.indexOf("    " + operation.method(), pathIndex);
        assertThat(methodIndex).as(operation.method() + " method must exist after " + operation.path()).isGreaterThanOrEqualTo(pathIndex);
        int nextPathIndex = openApi.indexOf("\n  /api/", pathIndex + 1);
        int endIndex = nextPathIndex >= 0 ? nextPathIndex : openApi.length();
        String pathBlock = openApi.substring(methodIndex, endIndex);
        assertThat(pathBlock).contains(operation.operationId());
        return pathBlock;
    }

    record OperationContract(String path, String method, String operationId) {}
}
