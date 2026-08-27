package kr.ac.knue.commonfoundation.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PermissionChangeLogMapperContractTest {
    private final String mapperXml;
    private final String migrationSql;
    private final String openApiYaml;

    PermissionChangeLogMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/audit/PermissionChangeLogMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        migrationSql = new ClassPathResource("db/migration/V20__basic29_permission_change_logs.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        openApiYaml = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void permissionChangeLogListReadsImmutableRowsWithDynamicFiltersOnly() {
        assertThat(mapperXml)
                .contains("select h.permission_history_id as \"permissionHistoryId\"")
                .contains("from permission_change_history h")
                .contains("h.before_value::text as \"beforeValue\"")
                .contains("h.after_value::text as \"afterValue\"")
                .contains("h.approver_user_id as \"approverUserId\"")
                .contains("h.changed_by as \"changedBy\"")
                .contains("h.reason as \"reason\"")
                .contains("<if test=\"criteria.targetType != null and criteria.targetType != ''\">")
                .contains("<if test=\"criteria.targetId != null and criteria.targetId != ''\">")
                .contains("<if test=\"criteria.approverUserId != null\">")
                .contains("<if test=\"criteria.changedBy != null\">")
                .contains("<if test=\"criteria.fromDate != null\">")
                .contains("<if test=\"criteria.toDate != null\">")
                .doesNotContain("update permission_change_history", "delete from permission_change_history")
                .doesNotContain("#{criteria.targetType} IS NULL", "COALESCE", "? IS NULL");
    }

    @Test
    void permissionChangeLogMigrationAddsApproverSeedIndexesMenuPermissionAndReadonlyComments() {
        assertThat(migrationSql)
                .contains("ALTER TABLE permission_change_history ADD COLUMN IF NOT EXISTS approver_user_id bigint")
                .contains("COMMENT ON COLUMN permission_change_history.approver_user_id")
                .contains("CREATE INDEX IF NOT EXISTS idx_permission_change_history_approver")
                .contains("SEED-PERMISSION-CHANGE-001")
                .contains("SCR-PERMISSION-CHANGE-LOG")
                .contains("/admin/audit/permission-change-logs")
                .contains("BASIC-29 권한변경 로그 조회 권한")
                .doesNotContain("DROP TABLE", "DELETE FROM permission_change_history", "UPDATE permission_change_history");
    }

    @Test
    void permissionChangeLogOpenApiContractIncludesPhaseSixOperationAndRequirementLinks() {
        assertThat(openApiYaml)
                .contains("/api/admin/audit/permission-change-logs:")
                .contains("operationId: listPermissionChangeLogs")
                .contains("- REQ-707", "- REQ-708", "- REQ-709", "- REQ-711", "- REQ-712")
                .contains("schema: {type: integer, enum: [20, 50, 100], default: 20}")
                .contains("schema: {type: string, enum: [ROLE, MENU, FUNCTION, DATA_SCOPE, TEMPORARY]}");
    }
}
