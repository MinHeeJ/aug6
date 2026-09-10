package kr.ac.knue.commonfoundation.basic59;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic59FoundationContractTest {
    @Test
    void phase1ConfirmsExistingSingleRepositoryRuntimeAndAuthReuseForT001T002T006() throws Exception {
        String migration = foundationMigration();
        CurrentUser teacher = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());
        Cookie sessionCookie = new Cookie(AuthController.SESSION_COOKIE, "SESSION-B59");

        assertThat(sessionCookie.getName()).isEqualTo("COMMON_FOUNDATION_SESSION");
        assertThat(teacher.roles()).containsExactly("R01");
        assertThat(Basic59FoundationContract.EXISTING_ROLE_CODES)
                .containsExactly("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
        assertThat(Basic59FoundationContract.AUTH_REUSE_POINTS)
                .contains("SessionCookie: COMMON_FOUNDATION_SESSION", "CurrentUser Principal", "EffectivePermissionService menu permission resolver");
        assertThat(classExists("kr.ac.knue.commonfoundation.auth.AuthenticationFilter")).isTrue();
        assertThat(classExists(EffectivePermissionService.class.getName())).isTrue();
        assertThat(migration)
                .contains("no-new-auth|no-new-user-table|no-new-org-table|no-new-role-table|no-second-compose")
                .doesNotContain("CREATE TABLE IF NOT EXISTS users")
                .doesNotContain("CREATE TABLE IF NOT EXISTS roles")
                .doesNotContain("CREATE TABLE IF NOT EXISTS organizations")
                .doesNotContain("INSERT INTO roles");
    }

    @Test
    void phase1RecordsMenuMaxEvidenceAndReservesFourConsecutiveLeafMenuIdsForT003T004() throws Exception {
        String previousMigration = new ClassPathResource("db/migration/V55__basic54_report_management_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String migration = foundationMigration();

        assertThat(previousMigration).contains("(560, NULL", "(561, NULL", "(562, NULL", "(563, NULL", "(564, NULL");
        assertThat(Basic59FoundationContract.MENU_ID_BASIS).isEqualTo("maxExistingMenuId=564; nextReservedMenuIds=565-568");
        assertThat(Basic59FoundationContract.RESERVED_MENU_IDS)
                .containsEntry("SCR-EVALUATION-ELEMENT-MGMT-ITEMS", 565L)
                .containsEntry("SCR-PARTICIPATION-RATE-OPERATION", 566L)
                .containsEntry("SCR-MANAGEMENT-ITEM-EVAL-SCORES", 567L)
                .containsEntry("SCR-COURSE-AREA-GROUP-GRADES", 568L);
        assertThat(migration)
                .contains("'SCR-EVALUATION-ELEMENT-MGMT-ITEMS', '/admin/evaluation-element-management-items', '/api/business/evaluation-element-management-items', 565")
                .contains("'SCR-PARTICIPATION-RATE-OPERATION', '/admin/participation-rate-operation-settings', '/api/business/participation-rate-operation-settings', 566")
                .contains("'SCR-MANAGEMENT-ITEM-EVAL-SCORES', '/admin/management-item-evaluation-scores', '/api/business/management-item-evaluation-scores', 567")
                .contains("'SCR-COURSE-AREA-GROUP-GRADES', '/evaluation/course-area-group-grades', '/api/business/course-area-group-grades', 568");
    }

    @Test
    void phase1ReservesOnlyFoundationMigrationAndRoutesWithoutLaterBusinessTablesForT005() throws Exception {
        String migration = foundationMigration();

        assertThat(Basic59FoundationContract.FOUNDATION_MIGRATION)
                .isEqualTo("V56__basic59_phase1_foundation_reservations.sql");
        assertThat(Basic59FoundationContract.uiRouteForApiPath("/api/business/course-area-group-grades/42"))
                .isEqualTo("/evaluation/course-area-group-grades");
        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS basic59_phase1_foundation_reservations")
                .contains("COMMENT ON TABLE basic59_phase1_foundation_reservations")
                .contains("ON CONFLICT (screen_id) DO UPDATE")
                .doesNotContain("CREATE TABLE IF NOT EXISTS evaluation_element_management_item_settings")
                .doesNotContain("CREATE TABLE IF NOT EXISTS participation_rate_operation_settings")
                .doesNotContain("CREATE TABLE IF NOT EXISTS management_item_score_settings")
                .doesNotContain("CREATE TABLE IF NOT EXISTS course_area_group_evaluation_grades");
    }

    private String foundationMigration() throws Exception {
        return new ClassPathResource("db/migration/" + Basic59FoundationContract.FOUNDATION_MIGRATION)
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
