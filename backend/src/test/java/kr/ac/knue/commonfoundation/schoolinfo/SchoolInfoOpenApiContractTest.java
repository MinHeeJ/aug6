package kr.ac.knue.commonfoundation.schoolinfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SchoolInfoOpenApiContractTest {
    private final String openApi;

    SchoolInfoOpenApiContractTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void openApiDeclaresSearchSchoolInfoContractAndVendorObligations() {
        String block = operationBlock("/api/admin/school-info:", "operationId: searchSchoolInfo");

        assertThat(block).contains(
                "get:",
                "SessionCookie",
                "x-roles:",
                "R09",
                "schoolName",
                "educationOfficeCode",
                "page",
                "size",
                "ApiResponse",
                "ApiError",
                "x-related-requirements",
                "REQ-1635",
                "x-required-tests",
                "INFO-000",
                "INFO-200",
                "URL 인코딩");
        assertThat(openApi).contains("SchoolInfoSearchResponse:", "SchoolInfoRow:");
    }

    private String operationBlock(String path, String operationId) {
        int pathIndex = openApi.indexOf(path);
        assertThat(pathIndex).as(path + " path must exist").isGreaterThanOrEqualTo(0);
        String block = openApi.substring(pathIndex, Math.min(openApi.length(), pathIndex + 5000));
        assertThat(block).contains(operationId);
        return block;
    }
}
