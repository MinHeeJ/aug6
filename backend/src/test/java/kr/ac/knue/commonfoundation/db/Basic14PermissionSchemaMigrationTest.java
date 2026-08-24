package kr.ac.knue.commonfoundation.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic14PermissionSchemaMigrationTest {
    private final String migrationSql;
    private final String historyMapperXml;

    Basic14PermissionSchemaMigrationTest() throws Exception {
        migrationSql = new ClassPathResource("db/migration/V5__basic14_permission_operations.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
        historyMapperXml = new ClassPathResource("mapper/permissionops/PermissionChangeHistoryMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void basic14PermissionTablesAreCreatedIdempotentlyWithCommentsAndConstraints() {
        for (String table : new String[] {"function_permissions", "period_permission_links", "temporary_permissions", "permission_change_history"}) {
            assertThat(migrationSql).contains("create table if not exists " + table);
            assertThat(migrationSql).contains("comment on table " + table);
        }
        assertThat(migrationSql).contains("unique (screen_id, role_code, function_type)");
        assertThat(migrationSql).contains("foreign key (role_code) references roles(role_code)");
        assertThat(migrationSql).contains("foreign key (function_permission_id) references function_permissions(function_permission_id)");
        assertThat(migrationSql).contains("foreign key (user_id) references users(user_id)");
        assertThat(migrationSql).contains("create index if not exists idx_function_permissions_lookup");
        assertThat(migrationSql).contains("create index if not exists idx_permission_change_history_filter");
    }

    @Test
    void migrationDoesNotRecreateOrAlterExistingRoleMenuPermissionOrUserRoleTables() {
        assertThat(migrationSql).doesNotContain("create table if not exists roles");
        assertThat(migrationSql).doesNotContain("create table if not exists user_roles");
        assertThat(migrationSql).doesNotContain("create table if not exists menu_permissions");
        assertThat(migrationSql).doesNotContain("alter table roles");
        assertThat(migrationSql).doesNotContain("alter table user_roles");
        assertThat(migrationSql).doesNotContain("alter table menu_permissions");
    }

    @Test
    void permissionChangeHistoryMapperIsPermanentRetentionReadOnlyForMutationPaths() {
        assertThat(historyMapperXml).contains("select id=\"listpermissionchangehistory\"");
        assertThat(historyMapperXml).contains("select id=\"countpermissionchangehistory\"");
        assertThat(historyMapperXml).contains("insert id=\"insertpermissionchangehistory\"");
        assertThat(historyMapperXml).doesNotContain("update id=");
        assertThat(historyMapperXml).doesNotContain("delete id=");
        assertThat(historyMapperXml).doesNotContain(" is null or ");
        assertThat(historyMapperXml).doesNotContain("coalesce(");
    }
}
