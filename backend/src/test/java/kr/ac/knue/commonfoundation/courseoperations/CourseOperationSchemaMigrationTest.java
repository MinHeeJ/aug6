package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CourseOperationSchemaMigrationTest {
    @Test
    void courseOperationMapperTablesAreCreatedByAnIdempotentMigration() throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V76__basic83_course_operation_achievements.sql");

        assertThat(migration.exists()).isTrue();
        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase();

        assertThat(sql)
                .contains("create table if not exists education_achievements")
                .contains("create table if not exists course_operation_achievement_details")
                .contains("foreign key (teacher_user_id) references users(user_id)")
                .contains("foreign key (achievement_id) references education_achievements(achievement_id)")
                .contains("create index if not exists idx_education_achievements_course_operation_search");
    }
}
