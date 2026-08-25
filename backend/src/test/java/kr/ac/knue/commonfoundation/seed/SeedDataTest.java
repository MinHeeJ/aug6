package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SeedDataTest {
    @Test
    void basic19MigrationMovesMenuScreensToRequestedParents() throws Exception {
        String sql = new ClassPathResource("db/migration/V9__basic19_menu_structure.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(sql).contains("(130, 100, 'middle', '메뉴 관리'");
        assertThat(sql).contains("(160, 100, 'middle', '개인정보 관리'");
        assertThat(sql).contains("where menu_id in (131, 132, 151)");
        assertThat(sql).contains("then 120");
        assertThat(sql).contains("where menu_id in (128, 129)");
        assertThat(sql).contains("then 160");
        assertThat(sql).contains("(161, 160, 'screen', '개인정보 처리이력'");
        assertThat(sql).contains("delete from menu_execution_info where menu_id = 130");
        assertThat(sql).contains("delete from menu_permissions where menu_id = 130");
    }

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
