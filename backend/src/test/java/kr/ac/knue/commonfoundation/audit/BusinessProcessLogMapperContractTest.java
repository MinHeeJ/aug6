package kr.ac.knue.commonfoundation.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BusinessProcessLogMapperContractTest {
    private final String mapperXml;
    private final String migrationSql;

    BusinessProcessLogMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/audit/BusinessProcessLogMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        migrationSql = new ClassPathResource("db/migration/V18__basic29_business_process_logs.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void businessProcessLogListReadsImmutableRowsWithDynamicFiltersOnly() {
        assertThat(mapperXml)
                .contains("select l.audit_log_id as \"auditLogId\"")
                .contains("from business_process_audit_logs l")
                .contains("l.before_state::text as \"beforeState\"")
                .contains("l.after_state::text as \"afterState\"")
                .contains("l.actor_user_id as \"actorUserId\"")
                .contains("l.result_status as \"resultStatus\"")
                .contains("l.request_id as \"requestId\"")
                .contains("<if test=\"criteria.actionType != null and criteria.actionType != ''\">")
                .contains("<if test=\"criteria.actorUserId != null\">")
                .contains("<if test=\"criteria.fromDate != null\">")
                .contains("<if test=\"criteria.toDate != null\">")
                .doesNotContain("update business_process_audit_logs", "delete from business_process_audit_logs")
                .doesNotContain("#{criteria.actionType} IS NULL", "COALESCE", "? IS NULL");
    }

    @Test
    void businessProcessLogMigrationProvidesSeedIndexesMenuPermissionAndReadonlyComments() {
        assertThat(migrationSql)
                .contains("COMMENT ON TABLE business_process_audit_logs")
                .contains("CREATE INDEX IF NOT EXISTS idx_business_process_audit_logs_actor")
                .contains("SEED-BUSINESS-AUDIT-001")
                .contains("SEED-BUSINESS-AUDIT-FAILURE")
                .contains("SCR-BUSINESS-PROCESS-LOG")
                .contains("/admin/audit/business-process-logs")
                .contains("BASIC-29 업무처리 로그 조회 권한")
                .doesNotContain("DROP TABLE", "DELETE FROM business_process_audit_logs", "UPDATE business_process_audit_logs");
    }
}
