package kr.ac.knue.commonfoundation.appealperiod;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AppealPeriodMapperContractTest {
    private final String mapperXml;

    AppealPeriodMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/appealperiod/AppealPeriodMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void mapperPersistsAppealPeriodColumnsAndUsesQuotedCamelCaseAliases() {
        assertThat(mapperXml)
                .contains("appeal_period_settings")
                .contains("ap.evaluation_year as \"evaluationYear\"")
                .contains("ap.college_organization_code as \"collegeOrganizationCode\"")
                .contains("ap.department_organization_code as \"departmentOrganizationCode\"")
                .contains("ap.appeal_start_at as \"appealStartAt\"")
                .contains("ap.appeal_end_at as \"appealEndAt\"")
                .contains("ap.handler_user_id as \"handlerUserId\"")
                .contains("ap.active_yn as \"activeYn\"")
                .contains("ap.created_at as \"createdAt\"")
                .contains("ap.updated_at as \"updatedAt\"");
    }

    @Test
    void mapperProvidesDynamicFiltersAndNoNullBoundPredicatePattern() {
        assertThat(mapperXml)
                .contains("<if test=\"criteria.normalizedEvaluationYear != null\">")
                .contains("<if test=\"criteria.normalizedCollegeOrganizationCode != null\">")
                .contains("<if test=\"criteria.normalizedDepartmentOrganizationCode != null\">")
                .contains("<if test=\"criteria.restrictOrganizationScope\">")
                .doesNotContain("#{criteria.evaluationYear} IS NULL", "? IS NULL", "COALESCE(:", "COALESCE(#{");
    }

    @Test
    void mapperBlocksOverlappingActivePeriodsAndValidatesHandlerScopeWithoutAppealContentWrites() {
        assertThat(mapperXml)
                .contains("countOverlappingAppealPeriods")
                .contains("tsrange(ap.appeal_start_at, ap.appeal_end_at, '[]')")
                .contains("existsHandlerUserForAppealPeriod")
                .contains("ur.role_code in ('R04','R09')")
                .contains("countAppealContentRowsForSetting")
                .doesNotContain("insert into appeal_contents", "update appeal_contents", "delete from appeal_contents");
    }
}
