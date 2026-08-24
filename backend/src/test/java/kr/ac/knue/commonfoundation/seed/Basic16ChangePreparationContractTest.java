package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic16ChangePreparationContractTest {
    private static final Set<String> BASELINE_ROLE_CODES = Set.of(
            "R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private static final Pattern ROLE_CODE_LITERAL = Pattern.compile("'R\\d{2}'");

    @Test
    void existingRoleSeedFixtureContainsOnlyR01ThroughR09() throws Exception {
        String seedSql = migrationSql("V2__common_foundation_seed.sql");

        Set<String> roleCodes = roleCodeLiterals(seedSql);

        assertThat(roleCodes).containsAll(BASELINE_ROLE_CODES);
        assertThat(roleCodes).doesNotContain("R10", "R11", "R99");
        assertThat(roleCodes).allSatisfy(roleCode -> assertThat(BASELINE_ROLE_CODES).contains(roleCode));
    }

    @Test
    void basic16PreparationDoesNotIntroduceAdditionalRoleRowsInLaterMigrations() throws Exception {
        String laterMigrationSql;
        try (Stream<Path> migrationFiles = Files.list(Path.of("src/main/resources/db/migration"))) {
            laterMigrationSql = migrationFiles
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .filter(path -> !path.getFileName().toString().equals("V2__common_foundation_seed.sql"))
                    .sorted()
                    .map(this::readString)
                    .collect(Collectors.joining("\n"));
        }

        assertThat(laterMigrationSql.toLowerCase()).doesNotContain("insert into roles");
        assertThat(roleCodeLiterals(laterMigrationSql)).allSatisfy(roleCode -> assertThat(BASELINE_ROLE_CODES).contains(roleCode));
    }

    private String migrationSql(String fileName) throws IOException {
        return new ClassPathResource("db/migration/" + fileName).getContentAsString(StandardCharsets.UTF_8);
    }

    private String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("migration fixture를 읽을 수 없습니다: " + path, exception);
        }
    }

    private Set<String> roleCodeLiterals(String sql) {
        Set<String> roleCodes = new LinkedHashSet<>();
        Matcher matcher = ROLE_CODE_LITERAL.matcher(sql);
        while (matcher.find()) {
            String literal = matcher.group();
            roleCodes.add(literal.substring(1, literal.length() - 1));
        }
        return roleCodes;
    }
}
