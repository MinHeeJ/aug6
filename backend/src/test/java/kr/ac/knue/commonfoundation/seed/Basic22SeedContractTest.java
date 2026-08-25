package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic22SeedContractTest {
    private final String migrationSql;

    Basic22SeedContractTest() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/*.sql");
        migrationSql = Arrays.stream(resources)
                .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                .map(this::readResource)
                .collect(Collectors.joining("\n"));
    }

    @Test
    void basic22ManagementScreensAreSeededAsMenusWithExecutionInfo() {
        assertSeededScreen("SCR-MESSAGE-MGMT", "/admin/messages", "메시지 관리");
        assertSeededScreen("SCR-NOTICE-MGMT", "/admin/notices", "공지사항 관리");
        assertSeededScreen("SCR-HELP-MGMT", "/admin/help-contents", "도움말 관리");
        assertSeededScreen("SCR-MANUAL-MGMT", "/admin/manuals", "매뉴얼 관리");

        assertThat(migrationSql)
                .contains("INSERT INTO menus", "INSERT INTO menu_execution_info", "BASIC-22");
    }

    @Test
    void r09RoleReceivesAllowMenuPermissionsForEveryBasic22ManagementScreen() {
        assertR09PermissionSeed("SCR-MESSAGE-MGMT", "/admin/messages");
        assertR09PermissionSeed("SCR-NOTICE-MGMT", "/admin/notices");
        assertR09PermissionSeed("SCR-HELP-MGMT", "/admin/help-contents");
        assertR09PermissionSeed("SCR-MANUAL-MGMT", "/admin/manuals");
    }

    private void assertSeededScreen(String screenId, String route, String menuName) {
        assertThat(migrationSql)
                .as(screenId + " menu seed must exist")
                .contains(screenId, route, menuName, "SCREEN", "SYSTEM", "ACTIVE");
    }

    private void assertR09PermissionSeed(String screenId, String route) {
        int screenIndex = migrationSql.indexOf(screenId);
        int routeIndex = migrationSql.indexOf(route);
        assertThat(screenIndex).as(screenId + " screen seed must exist before permission assertion").isGreaterThanOrEqualTo(0);
        assertThat(routeIndex).as(route + " route seed must exist before permission assertion").isGreaterThanOrEqualTo(0);
        assertThat(migrationSql)
                .as(screenId + " must be covered by R09 role menu permission seed")
                .contains("menu_permissions", "ROLE", "R09", "ALLOW");
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read migration resource: " + resource, exception);
        }
    }
}
