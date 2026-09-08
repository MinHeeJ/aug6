package kr.ac.knue.commonfoundation.menus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class MenuLocalizedSchemaMigrationTest {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Test
    void flywayMigrationAddsMenuNameEnColumnWithBusinessComment() throws IOException {
        String migrationSql = allMigrationSql();

        assertThat(migrationSql)
                .contains("alter table menus")
                .contains("add column if not exists menu_name_en")
                .doesNotContain("menu_name_en varchar(200) not null")
                .contains("comment on column menus.menu_name_en")
                .contains("영어 선택 시 메뉴 표시명");
    }

    @Test
    void flywayMigrationSeedsExistingMenusWithEnglishDisplayNames() throws IOException {
        String migrationSql = allMigrationSql();

        assertThat(migrationSql)
                .contains("update menus")
                .contains("set menu_name_en")
                .contains("where menu_name")
                .contains("system management")
                .contains("menu management")
                .contains("user and organization management");
    }

    private String allMigrationSql() throws IOException {
        return Arrays.stream(resolver.getResources("classpath*:db/migration/*.sql"))
                .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                .map(resource -> {
                    try {
                        return resource.getContentAsString(StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Flyway migration SQL을 읽을 수 없습니다.", exception);
                    }
                })
                .reduce("", (left, right) -> left + "\n" + right)
                .toLowerCase(Locale.ROOT);
    }
}
