package kr.ac.knue.commonfoundation.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SensitiveInformationAccessLogMapperContractTest {
    private final String mapperXml;
    private final String migrationSql;

    SensitiveInformationAccessLogMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/audit/SensitiveInformationAccessLogMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        migrationSql = new ClassPathResource("db/migration/V19__basic29_sensitive_information_access_logs.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void sensitiveInformationAccessLogListReadsImmutableRowsWithDynamicFiltersOnly() {
        assertThat(mapperXml)
                .contains("select l.access_log_id as \"accessLogId\"")
                .contains("from sensitive_information_access_logs l")
                .contains("l.information_type as \"informationType\"")
                .contains("l.viewer_user_id as \"viewerUserId\"")
                .contains("l.target_scope as \"targetScope\"")
                .contains("l.access_purpose as \"accessPurpose\"")
                .contains("l.purpose_source as \"purposeSource\"")
                .contains("l.access_result as \"accessResult\"")
                .contains("l.request_id as \"requestId\"")
                .contains("<if test=\"criteria.informationType != null and criteria.informationType != ''\">")
                .contains("<if test=\"criteria.viewerUserId != null\">")
                .contains("<if test=\"criteria.fromDate != null\">")
                .contains("<if test=\"criteria.toDate != null\">")
                .doesNotContain("protected_plain", "plain_value", "account_number_plain")
                .doesNotContain("update sensitive_information_access_logs", "delete from sensitive_information_access_logs")
                .doesNotContain("#{criteria.informationType} IS NULL", "COALESCE", "? IS NULL");
    }

    @Test
    void sensitiveInformationAccessLogMigrationProvidesSeedIndexesMenuPermissionAndReadonlyComments() {
        assertThat(migrationSql)
                .contains("CREATE TABLE IF NOT EXISTS sensitive_information_access_logs")
                .contains("COMMENT ON TABLE sensitive_information_access_logs")
                .contains("COMMENT ON COLUMN sensitive_information_access_logs.information_type IS 'PERSONAL_EVALUATION_RESULT:개인평가결과|SCORE_CALCULATION:점수산정|PERSONAL_INFORMATION:개인정보|ACCOUNT_INFORMATION:계좌정보'")
                .contains("CREATE INDEX IF NOT EXISTS idx_sensitive_information_access_logs_viewer")
                .contains("SEED-SENSITIVE-ACCESS-001")
                .contains("SCR-SENSITIVE-INFO-ACCESS-LOG")
                .contains("/admin/audit/sensitive-information-access-logs")
                .contains("BASIC-29 중요정보 조회 로그 조회 권한")
                .doesNotContain("protected_plain", "plain_value", "account_number_plain")
                .doesNotContain("DROP TABLE", "DELETE FROM sensitive_information_access_logs", "UPDATE sensitive_information_access_logs");
    }
}
