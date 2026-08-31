package kr.ac.knue.commonfoundation.basic32;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class Basic32MigrationGuardTest {
    private static final Pattern CREATE_TABLE_WITHOUT_IF_NOT_EXISTS = Pattern.compile("(?is)create\\s+table\\s+(?!if\\s+not\\s+exists)");
    private static final Pattern CREATE_INDEX_WITHOUT_IF_NOT_EXISTS = Pattern.compile("(?is)create\\s+(unique\\s+)?index\\s+(?!if\\s+not\\s+exists)");

    @Test
    void basic32SchemaMustUseOnlyIncrementalV21MigrationLocationForReq716() throws Exception {
        List<String> migrationNames = migrationNames();

        assertThat(migrationNames)
                .as("기존 V1~V20 migration은 재사용하고 BASIC-32 증분 DDL은 V21~V26__basic32... 위치만 사용한다.")
                .filteredOn(name -> name.toLowerCase().contains("basic32"))
                .allSatisfy(name -> assertThat(name).matches("V2[1-6]__basic32.*\\.sql"));
    }

    @Test
    void migrationDirectoryKeepsCommonFoundationBaselineBeforeBusinessSchemaForReq716() throws Exception {
        List<String> migrationNames = migrationNames();

        assertThat(migrationNames).contains("V1__common_foundation_schema.sql", "V2__common_foundation_seed.sql");
        assertThat(migrationNames).anySatisfy(name -> assertThat(name).startsWith("V20__basic29"));
        assertThat(migrationNames)
                .as("BASIC-32 phase 1 must not create another baseline schema or rewrite common foundation migrations")
                .noneSatisfy(name -> assertThat(name).startsWith("V1__basic32"));
    }

    @Test
    void allFlywayDdlRemainsIdempotentForPostgresqlRepeatedRuns() throws Exception {
        for (Resource resource : migrationResources()) {
            String sql = resource.getContentAsString(StandardCharsets.UTF_8);
            assertThat(CREATE_TABLE_WITHOUT_IF_NOT_EXISTS.matcher(sql).find())
                    .as(resource.getFilename() + " CREATE TABLE must use IF NOT EXISTS")
                    .isFalse();
            assertThat(CREATE_INDEX_WITHOUT_IF_NOT_EXISTS.matcher(sql).find())
                    .as(resource.getFilename() + " CREATE INDEX must use IF NOT EXISTS")
                    .isFalse();
        }
    }

    private List<String> migrationNames() throws IOException {
        return Arrays.stream(migrationResources())
                .map(Resource::getFilename)
                .sorted()
                .toList();
    }

    private Resource[] migrationResources() throws IOException {
        return new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
    }
}
