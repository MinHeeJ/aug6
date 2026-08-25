package kr.ac.knue.commonfoundation.helpcontents;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class HelpContentSchemaContractTest {
    private final String migrationSql;

    HelpContentSchemaContractTest() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/*.sql");
        migrationSql = Arrays.stream(resources)
                .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                .map(this::readResource)
                .collect(Collectors.joining("\n"));
    }

    @Test
    void helpContentsTableUsesUniqueScreenIdAndAuditMetadata() {
        assertThat(migrationSql)
                .contains("CREATE TABLE IF NOT EXISTS help_contents")
                .contains("screen_id varchar(100) NOT NULL UNIQUE")
                .contains("business_description text NOT NULL")
                .contains("input_criteria text NOT NULL")
                .contains("created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP")
                .contains("created_by bigint")
                .contains("updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP")
                .contains("updated_by bigint")
                .contains("COMMENT ON TABLE help_contents")
                .contains("CREATE INDEX IF NOT EXISTS idx_help_contents_screen_id");
    }

    @Test
    void helpContentsSeedProvidesExistingScreenAndDoesNotFallbackForMissingScreen() {
        assertThat(migrationSql)
                .contains("INSERT INTO help_contents")
                .contains("SCR-MESSAGE-MGMT")
                .doesNotContain("SCR-NO-HELP");
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read migration resource: " + resource, exception);
        }
    }
}
