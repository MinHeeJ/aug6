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
}
