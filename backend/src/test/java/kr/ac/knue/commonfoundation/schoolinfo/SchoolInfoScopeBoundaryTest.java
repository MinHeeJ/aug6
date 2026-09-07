package kr.ac.knue.commonfoundation.schoolinfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SchoolInfoScopeBoundaryTest {
    @Test
    void schoolInfoDoesNotAddPersistenceMapperOrStorageTable() throws Exception {
        Path root = Path.of("src/main");
        String allMainSources = Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("db/migration/V54__basic53_school_info_menu_seed.sql"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception exception) {
                        return "";
                    }
                })
                .reduce("", (left, right) -> left + "\n" + right);

        assertThat(allMainSources).doesNotContain("SchoolInfoMapper");
        assertThat(allMainSources).doesNotContain("school_info_history");
        assertThat(allMainSources).doesNotContain("CREATE TABLE IF NOT EXISTS school_info");
    }
}
