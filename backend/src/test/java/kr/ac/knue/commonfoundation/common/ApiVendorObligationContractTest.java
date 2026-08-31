package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

class ApiVendorObligationContractTest {
    @Test
    void openApiFixtureIsLoadedFromClasspathContracts() throws Exception {
        Map<String, Object> operation = operation("post", "/api/admin/excel-uploads");

        assertThat(operation).containsKeys("x-required-tests", "x-side-effects", "x-state-transitions");
        assertThat(normalized(operation)).contains("auth", "business", "happy", "side-effect", "validation",
                "excel_upload_files", "excel_upload_staging_rows", "excel_upload_errors", "excel_upload_histories",
                "uploaded", "validated");
    }

    @Test
    void writeOperationsDeclareConcreteRequiredTestsSideEffectsAndStateTransitions() throws Exception {
        assertObligation("delete", "/api/admin/user-roles/{assignmentId}", "user_roles");
        assertObligation("patch", "/api/admin/batch-executions/{executionId}/status", "auth", "business", "side-effect", "validation", "batch_executions", "execution_status", "operator_user_id", "request_id");
        assertObligation("post", "/api/admin/batch-definitions", "business", "batch_definitions", "batch_dependencies", "batch_parameters", "request_id", "draft_or_active");
        assertObligation("post", "/api/admin/batch-executions", "business", "batch_executions", "operator_user_id", "request_id", "waiting");
        assertObligation("post", "/api/admin/batch-executions/{executionId}/rerun", "auth", "business", "side-effect", "validation", "batch_executions", "original_execution_id", "rerun_running");
        assertObligation("post", "/api/admin/batch-retries", "auth", "business", "side-effect", "batch_retry_results", "original_execution_id", "request_id", "retry_execution_id", "retry_reason", "failed_target", "retry_running");
        assertObligation("post", "/api/admin/excel-downloads", "side-effect", "excel_download_jobs", "file_token", "requested");
        assertObligation("post", "/api/admin/excel-uploads", "auth", "business", "happy", "side-effect", "validation", "excel_upload_errors", "excel_upload_files", "excel_upload_histories", "excel_upload_staging_rows", "uploaded", "validated");
        assertObligation("post", "/api/admin/excel-uploads/{uploadId}/commit", "auth", "excel_upload_histories", "saved_count", "transaction", "committed", "rejected");
        assertObligation("post", "/api/admin/function-permissions/evaluate", "business", "side-effect", "permission_change_history", "decision", "requested");
        assertObligation("post", "/api/admin/manuals", "business", "table", "updated_at", "updated_by", "active", "pending");
        assertObligation("post", "/api/admin/notices", "auth", "table", "updated_at", "updated_by", "active", "pending");
        assertObligation("post", "/api/admin/security/active-sessions/{sessionId}/terminate", "business", "side-effect", "action_type", "business_process_audit_logs", "request_id", "session_terminate", "session_termination_history");
        assertObligation("post", "/api/admin/system-settings/base-years/{baseYear}/standards-preparation", "standard_year_preparation_history", "persisted");
        assertObligation("post", "/api/admin/system-settings/messages", "updated_at", "updated_by", "active", "pending");
        assertObligation("post", "/api/admin/temporary-permissions-create", "permission_change_history", "temporary_permissions");
        assertObligation("post", "/api/admin/user-roles", "user_roles");
        assertObligation("post", "/api/auth/login", "none");
        assertObligation("post", "/api/auth/logout", "business", "logged_out", "sessions");
        assertObligation("put", "/api/admin/code-groups/{groupId}/codes/{codeValue}/usage", "detail_codes", "persisted", "requested");
        assertObligation("put", "/api/admin/function-permissions-save", "business", "permission_change_history", "function_permissions", "persisted", "requested");
        assertObligation("put", "/api/admin/help-contents/{screenId}", "side-effect", "table", "updated_at", "updated_by", "active", "pending");
        assertObligation("put", "/api/admin/menus/exposure-save", "requested");
        assertObligation("put", "/api/admin/notices/{noticeId}", "business", "happy", "side-effect", "validation", "table", "updated_at", "updated_by", "active", "pending");
        assertObligation("put", "/api/admin/period-permissions-save", "permission_change_history", "period_permission_links", "persisted", "requested");
        assertObligation("put", "/api/admin/system-settings/base-year-current", "business", "base_year_settings", "persisted");
        assertObligation("put", "/api/admin/system-settings/common-values", "business", "common_settings", "persisted", "requested");
        assertObligation("put", "/api/admin/system-settings/messages/{messageCode}", "side-effect", "table", "updated_at", "updated_by", "active", "pending");
    }

    @Test
    void readOperationsDeclareRequestIdMetaAndSensitiveAccessBusinessObligations() throws Exception {
        assertObligation("get", "/api/admin/audit/business-process-logs", "meta");
        assertObligation("get", "/api/admin/audit/permission-change-logs", "meta", "requestid");
        assertObligation("get", "/api/admin/audit/sensitive-information-access-logs", "business", "access_purpose", "meta", "purpose_source", "sensitive_information_access_logs", "target_scope", "requestid");
        assertObligation("get", "/api/admin/security/active-sessions", "meta", "requestid");
        assertObligation("get", "/api/admin/security/session-termination-histories", "meta", "requestid");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> operation(String method, String path) throws Exception {
        ClassPathResource resource = new ClassPathResource("contracts/openapi.yaml");
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> document = new Yaml().load(inputStream);
            Map<String, Object> paths = (Map<String, Object>) document.get("paths");
            Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
            assertThat(pathItem).as(method.toUpperCase() + " " + path).isNotNull();
            Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);
            assertThat(operation).as(method.toUpperCase() + " " + path).isNotNull();
            return operation;
        }
    }

    private void assertObligation(String method, String path, String... tokens) throws Exception {
        String normalized = normalized(operation(method, path));
        assertThat(normalized).as(method.toUpperCase() + " " + path).contains(tokens);
    }

    private String normalized(Map<String, Object> operation) {
        return List.of(operation.get("x-required-tests"), operation.get("x-side-effects"), operation.get("x-state-transitions"))
                .toString()
                .replace(' ', '_')
                .toLowerCase();
    }
}
