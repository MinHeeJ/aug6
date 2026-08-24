package kr.ac.knue.commonfoundation.permissionops;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PermissionOperationsOpenApiContractTest {
    private final String openApi;

    PermissionOperationsOpenApiContractTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void saveFunctionPermissionsContractRequiresFunctionTypeAndR09Session() {
        assertOperationContract("/api/admin/function-permissions-save:", "put:", "operationId: saveFunctionPermissions",
                "screenId", "roleCode", "functionType", "permissionAllowed", "REQ-149", "REQ-152");
        assertThat(openApi).contains("validation: 필수 field 누락");
        assertThat(openApi).contains("security: [{SessionCookie: []}]");
        assertThat(openApi).contains("x-roles: [R09]");
    }

    @Test
    void savePeriodPermissionsContractRequiresPeriodRangeAndR09Session() {
        assertOperationContract("/api/admin/period-permissions-save:", "put:", "operationId: savePeriodPermissions",
                "businessPeriodId", "functionPermissionId", "effectiveStartAt", "effectiveEndAt", "REQ-153", "REQ-170");
        assertThat(openApi).contains("validation: 필수 field 누락");
        assertThat(openApi).contains("REQ-169");
    }

    @Test
    void createTemporaryPermissionContractRequiresTeacherWorkDataFunctionAndEffectiveDates() {
        assertOperationContract("/api/admin/temporary-permissions-create:", "post:", "operationId: createTemporaryPermission",
                "userId", "workDataRef", "functionType", "validStartAt", "validEndAt", "REQ-157");
        assertThat(openApi).contains("validation: 필수 field 누락");
        assertThat(openApi).contains("REQ-160");
    }

    @Test
    void listPermissionChangeHistoryContractIsReadOnlyPaginatedAndFilterable() {
        assertOperationContract("/api/admin/permission-history:", "get:", "operationId: listPermissionChangeHistory",
                "page", "size", "targetType", "targetId", "REQ-161", "REQ-171");
        assertThat(openApi).contains("operationId: listPermissionChangeHistory");
        assertThat(openApi).doesNotContain("name: changedBy", "fromChangedAt", "toChangedAt", "/api/admin/permission-history-delete");
    }

    private void assertOperationContract(String path, String method, String operationId, String... requiredSnippets) {
        int pathIndex = openApi.indexOf(path);
        assertThat(pathIndex).as(path + " path must exist").isGreaterThanOrEqualTo(0);
        String operationBlock = openApi.substring(pathIndex, Math.min(openApi.length(), pathIndex + 3500));
        assertThat(operationBlock).contains(method, operationId, "'200':", "'400':", "'401':", "'403':");
        assertThat(operationBlock).contains(requiredSnippets);
    }
}
