package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class BatchFoundationDiscoveryTest {
    @Test
    void sessionCookieAndR09AdminPermissionFoundationRemainTheReferenceForBatchAdminApis() throws Exception {
        String authControllerSource = source("src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java");
        String permissionServiceSource = source("src/main/java/kr/ac/knue/commonfoundation/permissions/EffectivePermissionService.java");
        String seedSql = new ClassPathResource("db/migration/V2__common_foundation_seed.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(authControllerSource)
                .contains("public static final String SESSION_COOKIE = \"COMMON_FOUNDATION_SESSION\"")
                .contains("httpOnly(true)")
                .contains("sameSite(\"Lax\")");
        assertThat(permissionServiceSource)
                .contains("roles.contains(\"R09\")")
                .contains("return true");
        assertThat(seedSql)
                .contains("('R09','시스템관리자'")
                .contains("SELECT user_id, 'R09', 'MANUAL'")
                .contains("INSERT INTO menu_permissions")
                .contains("SELECT 'ROLE', 'R09', menu_id, 'ALLOW'");
    }

    @Test
    void batchMenuSeedShouldBeAddedInANewMigrationAfterBasic22MenuSeedsWithoutChangingExistingRoleSeeds() throws Exception {
        String allMigrations = allMigrationSql();

        assertThat(allMigrations)
                .contains("SCR-MESSAGE-MGMT")
                .contains("SCR-NOTICE-MGMT")
                .contains("SCR-HELP-MGMT")
                .contains("SCR-MANUAL-MGMT");
        assertThat(allMigrations).contains("FROM menus\nWHERE screen_id IN ('SCR-MESSAGE-MGMT','SCR-NOTICE-MGMT','SCR-HELP-MGMT','SCR-MANUAL-MGMT')");
        assertThat(allMigrations).doesNotContain("('R10'", "role_code = 'R10'");
    }

    private String allMigrationSql() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
        return Arrays.stream(resources)
                .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                .map(this::readResource)
                .collect(Collectors.joining("\n"));
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read migration resource: " + resource, exception);
        }
    }

    private String source(String path) throws Exception {
        java.nio.file.Path localPath = java.nio.file.Path.of(path);
        if (java.nio.file.Files.exists(localPath)) {
            return java.nio.file.Files.readString(localPath, StandardCharsets.UTF_8);
        }
        return java.nio.file.Files.readString(java.nio.file.Path.of("backend").resolve(path), StandardCharsets.UTF_8);
    }
}
