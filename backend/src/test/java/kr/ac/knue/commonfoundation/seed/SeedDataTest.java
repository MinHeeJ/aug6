package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SeedDataTest {
    @Test
    void seedSqlContainsAdminRolesMenusPermissionsOrganizationsAndUsers() throws Exception {
        String sql = new ClassPathResource("db/migration/V2__common_foundation_seed.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        for (String role : new String[]{"R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09"}) {
            assertThat(sql).contains(role);
        }
        assertThat(sql).contains("admin");
        assertThat(sql).contains("SCR-USER-MGMT", "SCR-ORG-MGMT", "SCR-ROLE-MGMT", "SCR-USER-ROLE-MGMT");
        assertThat(sql).contains("SCR-MENU-PERMISSION-MGMT", "SCR-MENU-STRUCTURE-MGMT", "SCR-MENU-INFO-MGMT");
        assertThat(sql).contains("SCR-CODE-GROUP-MGMT", "SCR-DETAIL-CODE-MGMT");
        assertThat(sql).contains("menu_permissions", "korus_personnel_snapshots", "organizations");
    }
}
