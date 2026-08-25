package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApiVendorObligationCoverageTest {
    private final String openApi;

    ApiVendorObligationCoverageTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void userRoleMutationsDeclareStateTransitionsAndTests() {
        assertVendorObligation("delete", "/api/admin/user-roles/{assignmentId}", List.of("auth", "business", "happy", "side-effect", "validation"),
                List.of("user_roles"), List.of("user_roles"));
        assertVendorObligation("post", "/api/admin/user-roles", List.of("auth", "business", "happy", "side-effect", "validation"),
                List.of("user_roles"), List.of("none", "user_roles"));
    }

    @Test
    void functionAndPermissionMutationsDeclareBusinessSideEffectsAndStateTransitions() {
        assertVendorObligation("post", "/api/admin/function-permissions/evaluate", List.of("business", "side-effect"),
                List.of("permission_change_history"), List.of("decision", "requested"));
        assertVendorObligation("put", "/api/admin/function-permissions-save", List.of("business"),
                List.of("permission_change_history"), List.of("function_permissions", "persisted", "requested"));
        assertVendorObligation("post", "/api/admin/temporary-permissions-create", List.of("side-effect"),
                List.of("permission_change_history"), List.of("none", "temporary_permissions"));
        assertVendorObligation("put", "/api/admin/period-permissions-save", List.of("side-effect"),
                List.of("permission_change_history"), List.of("period_permission_links", "persisted", "requested"));
    }

    @Test
    void contentManagementMutationsDeclareAuditTableSideEffectsAndActiveTransitions() {
        assertVendorObligation("post", "/api/admin/manuals", List.of("business"),
                List.of("table", "updated_at", "updated_by"), List.of("active", "pending"));
        assertVendorObligation("post", "/api/admin/notices", List.of("auth"),
                List.of("table", "updated_at", "updated_by"), List.of("active", "pending"));
        assertVendorObligation("put", "/api/admin/help-contents/{screenId}", List.of("side-effect"),
                List.of("table", "updated_at", "updated_by"), List.of("active", "pending"));
        assertVendorObligation("put", "/api/admin/notices/{noticeId}", List.of("business", "happy", "side-effect", "validation"),
                List.of("table", "updated_at", "updated_by"), List.of("active", "pending"));
    }

    @Test
    void systemSettingMessageMutationsDeclareFullRequiredTestsAuditSideEffectsAndTransitions() {
        assertVendorObligation("post", "/api/admin/system-settings/messages", List.of("auth", "business", "happy", "side-effect", "validation"),
                List.of("table", "updated_at", "updated_by"), List.of("active", "pending"));
        assertVendorObligation("put", "/api/admin/system-settings/messages/{messageCode}", List.of("side-effect"),
                List.of("table", "updated_at", "updated_by"), List.of("active", "pending"));
    }

    @Test
    void systemSettingBaseYearCommonAndCodeMutationsDeclarePersistenceTransitions() {
        assertVendorObligation("post", "/api/admin/system-settings/base-years/{baseYear}/standards-preparation", List.of("side-effect"),
                List.of("standard_year_preparation_history"), List.of("persisted"));
        assertVendorObligation("put", "/api/admin/code-groups/{groupId}/codes/{codeValue}/usage", List.of("side-effect"),
                List.of("detail_codes"), List.of("persisted", "requested"));
        assertVendorObligation("put", "/api/admin/menus/exposure-save", List.of("side-effect"),
                List.of("menus"), List.of("requested"));
        assertVendorObligation("put", "/api/admin/system-settings/base-year-current", List.of("business"),
                List.of("base_year_settings"), List.of("persisted"));
        assertVendorObligation("put", "/api/admin/system-settings/common-values", List.of("business"),
                List.of("common_settings"), List.of("persisted", "requested"));
    }

    @Test
    void authOperationsDeclareSessionStateTransitionsAndBusinessTests() {
        assertVendorObligation("post", "/api/auth/login", List.of("auth", "business", "happy", "side-effect", "validation"),
                List.of("sessions"), List.of("none"));
        assertVendorObligation("post", "/api/auth/logout", List.of("business"),
                List.of("sessions"), List.of("logged_out", "sessions"));
    }

    private void assertVendorObligation(String method, String path, List<String> requiredTests,
            List<String> sideEffects, List<String> stateTransitions) {
        String block = operationBlock(method, path);
        if (!requiredTests.isEmpty()) {
            assertThat(block).contains("x-required-tests");
            requiredTests.forEach(test -> assertThat(block.toLowerCase()).contains(test));
        }
        if (!sideEffects.isEmpty()) {
            assertThat(block).contains("x-side-effects");
            sideEffects.forEach(sideEffect -> assertThat(sideEffect).isNotBlank());
        }
        if (!stateTransitions.isEmpty()) {
            assertThat(block).contains("x-state-transitions");
            stateTransitions.forEach(transition -> assertThat(transition).isNotBlank());
        }
    }

    private String operationBlock(String method, String path) {
        int pathIndex = openApi.indexOf("  " + path + ":");
        assertThat(pathIndex).as(path + " path must exist in classpath OpenAPI fixture").isGreaterThanOrEqualTo(0);
        int methodIndex = openApi.indexOf("    " + method + ":", pathIndex);
        assertThat(methodIndex).as(method + " " + path + " operation must exist").isGreaterThanOrEqualTo(0);
        int nextMethodIndex = openApi.length();
        for (String candidate : List.of("    get:\n", "    post:\n", "    put:\n", "    patch:\n", "    delete:\n")) {
            int candidateIndex = openApi.indexOf(candidate, methodIndex + 1);
            if (candidateIndex > methodIndex && candidateIndex < nextMethodIndex) {
                nextMethodIndex = candidateIndex;
            }
        }
        int nextPathIndex = openApi.indexOf("\n  /api/", methodIndex + 1);
        int endIndex = nextPathIndex >= 0 ? Math.min(nextMethodIndex, nextPathIndex) : nextMethodIndex;
        return openApi.substring(methodIndex, endIndex);
    }
}
