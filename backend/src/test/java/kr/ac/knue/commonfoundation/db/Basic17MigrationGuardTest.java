package kr.ac.knue.commonfoundation.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic17MigrationGuardTest {
    private static final Set<String> BASELINE_ROLE_CODES = Set.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private static final Pattern ROLE_LITERAL = Pattern.compile("'(?<role>R\\d{2})'");

    private final List<String> allMigrationSql;
    private final List<String> basic17MigrationSql;

    Basic17MigrationGuardTest() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        allMigrationSql = readSql(resolver.getResources("classpath*:db/migration/*.sql"));
        basic17MigrationSql = readSql(resolver.getResources("classpath*:db/migration/*basic17*.sql"));
    }

    @Test
    void baselineRoleSeedContainsOnlyExistingR01ThroughR09RoleCodesForReq175() {
        String combinedSql = String.join("\n", allMigrationSql);

        for (String roleCode : BASELINE_ROLE_CODES) {
            assertThat(combinedSql).contains("'" + roleCode + "'");
        }
        assertThat(extractRoleLiterals(combinedSql)).containsOnlyElementsOf(BASELINE_ROLE_CODES);
    }

    @Test
    void basic17MigrationsMustOnlyReferenceExistingRolesWithoutCreatingRenamingOrAssigningRolesForReq175() {
        String combinedSql = String.join("\n", basic17MigrationSql).toLowerCase();

        assertThat(combinedSql).doesNotContain("insert into roles");
        assertThat(combinedSql).doesNotContain("update roles");
        assertThat(combinedSql).doesNotContain("insert into user_roles");
        assertThat(combinedSql).doesNotContain("update user_roles");
        assertThat(combinedSql).doesNotContain("delete from roles");
        assertThat(combinedSql).doesNotContain("delete from user_roles");
        assertThat(extractRoleLiterals(combinedSql.toUpperCase())).containsOnlyElementsOf(BASELINE_ROLE_CODES);
    }

    private List<String> readSql(Resource[] resources) throws IOException {
        return Arrays.stream(resources)
                .map(this::readResource)
                .toList();
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("migration SQL을 읽을 수 없습니다.", exception);
        }
    }

    private List<String> extractRoleLiterals(String sql) {
        Matcher matcher = ROLE_LITERAL.matcher(sql);
        return matcher.results()
                .map(result -> result.group(1))
                .toList();
    }
}
