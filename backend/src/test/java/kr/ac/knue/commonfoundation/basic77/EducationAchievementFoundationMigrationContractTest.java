package kr.ac.knue.commonfoundation.basic77;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Defines the durable persistence boundary required before education-achievement transition APIs are added.
 */
class EducationAchievementFoundationMigrationContractTest {
    private static final Path MIGRATION_DIRECTORY = Path.of(
            "src", "main", "resources", "db", "migration");

    @Test
    void rejectionTransitionHasDurableReasonAndProcessorHistoryForLectureEvaluationAchievements() throws IOException {
        Optional<Path> basic77Migration;
        try (Stream<Path> migrations = Files.list(MIGRATION_DIRECTORY)) {
            basic77Migration = migrations
                    .filter(path -> path.getFileName().toString().contains("basic77"))
                    .findFirst();
        }

        assertThat(basic77Migration)
                .as("BASIC-77 incremental Flyway migration must establish education achievement status history")
                .isPresent();

        String migration = Files.readString(basic77Migration.orElseThrow(), StandardCharsets.UTF_8).toLowerCase();
        assertThat(migration).contains(
                "create table if not exists lecture_evaluation_achievements",
                "create table if not exists education_achievement_status_histories",
                "previous_status",
                "next_status",
                "action_type",
                "reason_code",
                "opinion",
                "processed_by",
                "processed_at",
                "lecture_evaluation",
                "b77-le-001");
    }
}
