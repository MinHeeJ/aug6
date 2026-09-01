package kr.ac.knue.commonfoundation.businessperiod;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BusinessPeriodBaselineVerificationTest {
    private static final List<String> BASELINE_ROLE_CODES = List.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");

    @Test
    void phase1VerifiesExistingR01ToR09RoleSeedIsPresentWithoutAddingBusinessPeriodRoles() throws Exception {
        String seedSql = seedSql();

        for (String roleCode : BASELINE_ROLE_CODES) {
            assertThat(seedSql).contains("'" + roleCode + "'");
        }
        assertThat(seedSql).contains("('R09','시스템관리자'");
        assertThat(seedSql).contains("INSERT INTO user_roles");
        assertThat(seedSql).contains("SELECT user_id, 'R09'");
        assertThat(seedSql).doesNotContain("BUSINESS_PERIOD_ADMIN");
        assertThat(seedSql).doesNotContain("PERIOD_ADMIN");
    }

    @Test
    void phase1VerifiesR09MenuPermissionSeedReusesExistingRoleTarget() throws Exception {
        String seedSql = seedSql();

        assertThat(seedSql).contains("INSERT INTO menu_permissions");
        assertThat(seedSql).contains("SELECT 'ROLE', 'R09', menu_id, 'ALLOW'");
        assertThat(seedSql).doesNotContain("SELECT 'ROLE', 'BUSINESS_PERIOD_ADMIN'");
    }

    @Test
    void phase1VerifiesSessionCookieAndServerMenuGuardRemainTheAccessBoundary() throws Exception {
        String filter = readSourceFile("kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java");
        String authController = readSourceFile("kr/ac/knue/commonfoundation/auth/AuthController.java");

        assertThat(authController).contains("SESSION_COOKIE = \"COMMON_FOUNDATION_SESSION\"");
        assertThat(authController).contains("ResponseCookie.from(SESSION_COOKIE, session.sessionId())");
        assertThat(filter).contains("AuthController.SESSION_COOKIE.equals(cookie.getName())");
        assertThat(filter).contains("path.startsWith(\"/api/admin/\") || path.startsWith(\"/api/business/\")");
        assertThat(filter).contains("permissionService.canAccess(user.userId(), user.roles(), pathToUiRoute(path))");
        assertThat(filter).contains("writeError(response, HttpServletResponse.SC_FORBIDDEN");
    }

    @Test
    void phase1VerifiesHealthEndpointBaselineEnvelope() {
        HealthController controller = new HealthController();

        var response = controller.health();

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsEntry("status", "UP");
        assertThat(response.data()).containsEntry("service", "common-foundation");
    }

    private String seedSql() throws Exception {
        return new ClassPathResource("db/migration/V2__common_foundation_seed.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String readSourceFile(String relativeJavaPath) throws Exception {
        Path backendRelative = Path.of("src/main/java", relativeJavaPath);
        if (Files.exists(backendRelative)) {
            return Files.readString(backendRelative, StandardCharsets.UTF_8);
        }
        return Files.readString(Path.of("backend/src/main/java", relativeJavaPath), StandardCharsets.UTF_8);
    }
}
