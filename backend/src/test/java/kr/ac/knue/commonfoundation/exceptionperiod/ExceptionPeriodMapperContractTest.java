package kr.ac.knue.commonfoundation.exceptionperiod;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExceptionPeriodMapperContractTest {
    private final String mapperXml;

    ExceptionPeriodMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/exceptionperiod/ExceptionPeriodMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void mapperPersistsExceptionPeriodColumnsAndUsesQuotedCamelCaseAliases() {
        assertThat(mapperXml)
                .contains("exception_period_settings")
                .contains("ep.evaluation_year as \"evaluationYear\"")
                .contains("ep.teacher_user_id as \"teacherUserId\"")
                .contains("ep.area_code as \"areaCode\"")
                .contains("ep.target_function_code as \"targetFunctionCode\"")
                .contains("ep.exception_start_at as \"exceptionStartAt\"")
                .contains("ep.exception_end_at as \"exceptionEndAt\"")
                .contains("ep.approval_reason as \"approvalReason\"")
                .contains("ep.updated_at as \"updatedAt\"");
    }

    @Test
    void mapperProvidesTargetFiltersDynamicallyAndAvoidsNullBoundPredicatePattern() {
        assertThat(mapperXml)
                .contains("<if test=\"criteria.teacherUserId != null\">")
                .contains("<if test=\"criteria.normalizedAreaCode != null\">")
                .contains("<if test=\"criteria.normalizedTargetFunctionCode != null\">")
                .doesNotContain("#{criteria.evaluationYear} IS NULL", "? IS NULL", "COALESCE(:", "COALESCE(#{");
    }

    @Test
    void mapperBlocksOverlappingActivePeriodsAndReadsBasic35ModificationFixtureWithoutChangingGeneralPeriods() {
        assertThat(mapperXml)
                .contains("countOverlappingExceptionPeriods")
                .contains("tsrange(ep.exception_start_at, ep.exception_end_at, '[]')")
                .contains("findActiveExceptionPeriodForModification")
                .contains("countActiveModificationPeriods")
                .contains("from modification_period_settings mp")
                .doesNotContain("insert into modification_period_settings", "update modification_period_settings", "delete from modification_period_settings");
    }
}
