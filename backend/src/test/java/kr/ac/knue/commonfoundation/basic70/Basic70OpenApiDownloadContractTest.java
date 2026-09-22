package kr.ac.knue.commonfoundation.basic70;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Locks the three BASIC-70 Excel endpoints to the durable OpenAPI fixture so the binary
 * response, session security, role scope, and allowed paging options cannot drift apart.
 */
class Basic70OpenApiDownloadContractTest {

    @Test
    void basic70ExcelDownloadsRemainBinarySessionProtectedAndLimitedToSettingsAdministrators() throws IOException {
        ClassPathResource fixture = new ClassPathResource("contracts/openapi.yaml");
        assertThat(fixture.exists()).isTrue();
        String openApi = new String(fixture.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertDownloadOperation(openApi,
                "/api/admin/evaluation-element-management-item-settings/download:",
                "downloadEvaluationElementManagementItemSettings");
        assertDownloadOperation(openApi,
                "/api/admin/participation-allocation-rate-settings/download:",
                "downloadParticipationAllocationRateSettings");
        assertDownloadOperation(openApi,
                "/api/admin/management-item-evaluation-score-settings/download:",
                "downloadManagementItemEvaluationScoreSettings");
    }

    private void assertDownloadOperation(String openApi, String path, String operationId) {
        int start = openApi.indexOf(path);
        assertThat(start).as("OpenAPI path %s", path).isGreaterThanOrEqualTo(0);
        int end = openApi.indexOf("\n  /api/", start + path.length());
        String operation = openApi.substring(start, end < 0 ? openApi.length() : end);

        assertThat(operation)
                .contains("get:", "operationId: " + operationId, "SessionCookie", "x-roles: [R04, R09]",
                        "pageSize", "enum: [20, 50, 100]",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "format: binary", "'401'", "'403'");
    }
}
