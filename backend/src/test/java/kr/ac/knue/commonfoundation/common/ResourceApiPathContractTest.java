package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ResourceApiPathContractTest {
    private static final List<String> RESOURCE_PATHS = List.of(
            "/api/admin/users",
            "/api/admin/users/{userId}/account",
            "/api/admin/users/{userId}/roles",
            "/api/admin/organizations",
            "/api/admin/organizations/tree",
            "/api/admin/organizations/{organizationCode}/parent-relations",
            "/api/admin/roles",
            "/api/admin/roles/{roleCode}",
            "/api/admin/user-roles",
            "/api/admin/user-roles/{assignmentId}",
            "/api/admin/menu-permissions",
            "/api/admin/menus/tree",
            "/api/admin/menus/{menuId}/parent",
            "/api/admin/menus/reorder",
            "/api/admin/menus/{menuId}/execution",
            "/api/admin/code-groups",
            "/api/admin/code-groups/{groupId}",
            "/api/admin/code-groups/{groupId}/codes",
            "/api/admin/code-groups/{groupId}/codes/{codeValue}",
            "/api/admin/menus/exposure",
            "/api/admin/menus/exposure-save",
            "/api/admin/code-groups/{groupId}/codes-usage",
            "/api/admin/code-groups/{groupId}/codes/{codeValue}/usage",
            "/api/admin/system-settings/common",
            "/api/admin/system-settings/common-values",
            "/api/admin/system-settings/base-years",
            "/api/admin/system-settings/base-year-current",
            "/api/admin/system-settings/base-years/{baseYear}/standards-preparation",
            "/api/admin/position-assignments",
            "/api/admin/position-assignments/{positionAssignmentId}",
            "/api/admin/duty-assignments",
            "/api/admin/duty-assignments/{dutyAssignmentId}",
            "/api/admin/data-scope-rules",
            "/api/admin/function-permissions",
            "/api/admin/function-permissions-save",
            "/api/admin/function-permissions/evaluate",
            "/api/admin/period-permissions",
            "/api/admin/period-permissions-save");

    @Test
    void openApiUsesResourcePathsInsteadOfEndpointPerRequirementPaths() throws Exception {
        String contract = new String(new ClassPathResource("contracts/openapi.yaml").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        for (String resourcePath : RESOURCE_PATHS) {
            assertThat(contract).contains("  " + resourcePath + ":");
        }

        Matcher pathMatcher = Pattern.compile("(?m)^  (/api/[^:]+):$").matcher(contract);
        while (pathMatcher.find()) {
            String path = pathMatcher.group(1);
            assertThat(path)
                    .as("OpenAPI path must be a domain resource path, not endpoint-per-requirement: %s", path)
                    .doesNotContain("REQ-")
                    .doesNotContain("CMN-FR-")
                    .doesNotContain("SCR-")
                    .doesNotContain("requirement");
        }
    }
}
