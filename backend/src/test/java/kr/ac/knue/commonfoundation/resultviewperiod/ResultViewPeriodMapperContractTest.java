package kr.ac.knue.commonfoundation.resultviewperiod;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ResultViewPeriodMapperContractTest {
    private final String mapperXml;

    ResultViewPeriodMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/resultviewperiod/ResultViewPeriodMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void mapperPersistsResultViewPeriodColumnsAndUsesQuotedCamelCaseAliases() {
        assertThat(mapperXml)
                .contains("result_view_period_settings")
                .contains("rv.evaluation_year as \"evaluationYear\"")
                .contains("rv.college_organization_code as \"collegeOrganizationCode\"")
                .contains("rv.department_organization_code as \"departmentOrganizationCode\"")
                .contains("rv.view_start_at as \"viewStartAt\"")
                .contains("rv.view_end_at as \"viewEndAt\"")
                .contains("rv.visibility_scope as \"visibilityScope\"")
                .contains("rv.active_yn as \"activeYn\"")
                .contains("rv.updated_at as \"updatedAt\"");
    }

    @Test
    void mapperProvidesDynamicFiltersAndNoNullBoundPredicatePattern() {
        assertThat(mapperXml)
                .contains("<if test=\"criteria.normalizedEvaluationYear != null\">")
                .contains("<if test=\"criteria.normalizedCollegeOrganizationCode != null\">")
                .contains("<if test=\"criteria.normalizedDepartmentOrganizationCode != null\">")
                .contains("<if test=\"criteria.normalizedVisibilityScope != null\">")
                .contains("<if test=\"criteria.restrictOrganizationScope\">")
                .doesNotContain("#{criteria.evaluationYear} IS NULL", "? IS NULL", "COALESCE(:", "COALESCE(#{");
    }

    @Test
    void mapperBlocksOverlappingActivePeriodsAndDoesNotModifyScoresOrConfirmations() {
        assertThat(mapperXml)
                .contains("countOverlappingResultViewPeriods")
                .contains("tsrange(rv.view_start_at, rv.view_end_at, '[]')")
                .contains("findActiveResultViewPeriodForAccess")
                .doesNotContain("insert into evaluation_scores", "update evaluation_scores", "delete from evaluation_scores")
                .doesNotContain("final_confirm", "confirmation_cancel", "cancel_confirmation");
    }
}
