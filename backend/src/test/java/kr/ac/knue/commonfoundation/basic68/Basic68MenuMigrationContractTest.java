package kr.ac.knue.commonfoundation.basic68;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Basic68MenuMigrationContractTest {
    @Test
    void basic68MenuMigrationAddsTheCourseAreaQueryRouteWithoutReusingAnExistingMenuId() throws IOException {
        List<Path> basic68Migrations;
        try (Stream<Path> paths = Files.list(Path.of("src", "main", "resources", "db", "migration"))) {
            basic68Migrations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("V[0-9_]+__basic68_.*\\.sql"))
                    .toList();
        }

        assertThat(basic68Migrations)
                .as("BASIC-68 menu migration")
                .hasSize(1);

        String migrationSql = Files.readString(basic68Migrations.get(0), StandardCharsets.UTF_8).toLowerCase();
        assertThat(migrationSql)
                .contains("insert into menus")
                .contains("scr-course-area-group-grade-query")
                .contains("/faculty/course-area-group-grades")
                .contains("insert into menu_execution_info")
                .contains("insert into menu_permissions")
                .contains("where not exists")
                .doesNotContain("on conflict (menu_id)");
    }
}
