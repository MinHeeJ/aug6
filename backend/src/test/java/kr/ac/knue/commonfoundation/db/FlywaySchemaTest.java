package kr.ac.knue.commonfoundation.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywaySchemaTest {
    private final String schemaSql;

    FlywaySchemaTest() throws Exception {
        schemaSql = new ClassPathResource("db/migration/V1__common_foundation_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void requiredTablesDeclareOperationalMetadataColumns() {
        String[] tables = {
                "users", "korus_personnel_snapshots", "organizations", "organization_relations",
                "organization_relation_history", "organization_user_mappings", "roles", "user_roles", "menus", "menu_permissions",
                "code_groups", "detail_codes", "sessions"
        };

        for (String table : tables) {
            assertThat(schemaSql).contains("create table if not exists " + table);
            assertThat(schemaSql).contains("comment on table " + table);
        }
        assertThat(schemaSql).contains("created_at timestamp");
        assertThat(schemaSql).contains("updated_at timestamp");
        assertThat(schemaSql).contains("system_use_yn varchar(1)");
        assertThat(schemaSql).contains("status varchar(20)");
    }

    @Test
    void requiredForeignKeysAndIndexesAreDeclaredIdempotently() {
        assertThat(schemaSql).contains("foreign key (user_id) references users(user_id)");
        assertThat(schemaSql).contains("foreign key (role_code) references roles(role_code)");
        assertThat(schemaSql).contains("foreign key (menu_id) references menus(menu_id)");
        assertThat(schemaSql).contains("create index if not exists");
    }

    @Test
    void basic5ChangeTablesDeclareRequiredFieldsAndComments() {
        assertThat(schemaSql)
                .contains("create table if not exists menu_usage_settings")
                .contains("menu_id bigint")
                .contains("system_use_yn varchar(1)")
                .contains("exposure_start_at timestamp")
                .contains("exposure_end_at timestamp")
                .contains("comment on table menu_usage_settings")
                .contains("comment on column menu_usage_settings.system_use_yn is 'y:사용|n:미사용'");

        assertThat(schemaSql)
                .contains("create table if not exists common_system_settings")
                .contains("setting_key varchar(100)")
                .contains("setting_value varchar(200)")
                .contains("session_idle_minutes")
                .contains("page_size")
                .contains("default_search_period")
                .contains("bulk_query_threshold")
                .contains("long_task_notice_threshold")
                .contains("comment on table common_system_settings");

        assertThat(schemaSql)
                .contains("create table if not exists evaluation_year_settings")
                .contains("current_evaluation_year integer")
                .contains("default_search_year integer")
                .contains("comment on table evaluation_year_settings");

        assertThat(schemaSql)
                .contains("create table if not exists evaluation_year_preparations")
                .contains("target_year integer")
                .contains("copy_requested_yn varchar(1)")
                .contains("reset_requested_yn varchar(1)")
                .contains("comment on column evaluation_year_preparations.copy_requested_yn is 'y:복사요청|n:복사미요청'")
                .contains("comment on column evaluation_year_preparations.reset_requested_yn is 'y:초기화요청|n:초기화미요청'");
    }

    @Test
    void basic5ChangeSchemaIsIdempotentAndIndexedForLookupPaths() {
        assertThat(schemaSql)
                .contains("foreign key (menu_id) references menus(menu_id)")
                .contains("create index if not exists idx_menu_usage_settings_period")
                .contains("create index if not exists idx_common_system_settings_key")
                .contains("create index if not exists idx_evaluation_year_preparations_target_year");
    }
}
