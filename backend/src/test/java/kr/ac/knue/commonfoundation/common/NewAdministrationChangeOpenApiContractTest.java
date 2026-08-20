package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NewAdministrationChangeOpenApiContractTest {
    private final String contract;

    NewAdministrationChangeOpenApiContractTest() throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        assertThat(openApi.exists()).isTrue();
        contract = openApi.getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void openApiFixtureDeclaresEightNewSystemAdministrationOperations() {
        assertThat(contract)
                .contains("/api/admin/menus/usage-settings:")
                .contains("operationId: listMenuUsageSettings")
                .contains("operationId: saveMenuUsageSettings")
                .contains("/api/admin/code-groups/{groupId}/codes/usage-settings:")
                .contains("operationId: listDetailCodeUsageSettings")
                .contains("operationId: saveDetailCodeUsageSettings")
                .contains("/api/admin/system-settings/common:")
                .contains("operationId: getCommonSystemSettings")
                .contains("operationId: saveCommonSystemSettings")
                .contains("/api/admin/system-settings/evaluation-years:")
                .contains("operationId: getEvaluationYearSettings")
                .contains("operationId: saveEvaluationYearSettings");
    }

    @Test
    void newOperationsRequireSessionCookieR09AndApiErrorResponses() {
        String[] operationIds = {
                "listMenuUsageSettings", "saveMenuUsageSettings",
                "listDetailCodeUsageSettings", "saveDetailCodeUsageSettings",
                "getCommonSystemSettings", "saveCommonSystemSettings",
                "getEvaluationYearSettings", "saveEvaluationYearSettings"
        };

        for (String operationId : operationIds) {
            int index = contract.indexOf("operationId: " + operationId);
            assertThat(index).as(operationId + " operation contract exists").isGreaterThanOrEqualTo(0);
            String operationBlock = contract.substring(index, Math.min(contract.length(), index + 2200));
            assertThat(operationBlock)
                    .as(operationId + " uses common security/error contract")
                    .contains("'401':")
                    .contains("'403':")
                    .contains("SessionCookie")
                    .contains("R09");
        }
    }

    @Test
    void mutatingOperationsDeclareRequestSchemasAndRequiredBusinessSideEffectTests() {
        assertThat(contract)
                .contains("$ref: '#/components/schemas/MenuUsageSettingsRequest'")
                .contains("$ref: '#/components/schemas/DetailCodeUsageSettingsRequest'")
                .contains("$ref: '#/components/schemas/CommonSystemSettingsRequest'")
                .contains("$ref: '#/components/schemas/EvaluationYearSettingsRequest'")
                .contains("menu_usage_settings row 저장")
                .contains("detail_codes 사용여부와 적용기간만 갱신")
                .contains("common_system_settings row 갱신")
                .contains("evaluation_year_settings/evaluation_year_preparations row 갱신")
                .contains("x-required-tests");
    }
}
