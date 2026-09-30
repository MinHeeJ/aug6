package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EducationAchievementFoundationMigrationTest {
    @Test
    void foundationMigrationCreatesLectureEvaluationAndStatusHistoryWithLifecycleGuards() throws Exception {
        String sql = migration("V60__basic79_education_achievement_foundation.sql");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS lecture_evaluation_achievements")
                .contains("CREATE TABLE IF NOT EXISTS education_achievement_status_histories")
                .contains("EVALUATION_CONFIRMED")
                .contains("deleted_yn char(1)")
                .contains("COMMENT ON TABLE lecture_evaluation_achievements")
                .contains("COMMENT ON TABLE education_achievement_status_histories")
                .contains("CREATE INDEX IF NOT EXISTS idx_lecture_evaluation_achievements_search")
                .contains("CREATE INDEX IF NOT EXISTS idx_education_achievement_status_histories_achievement");
    }

    @Test
    void seedMigrationProvidesThreeLectureEvaluationFixtureCases() throws Exception {
        String sql = migration("V61__basic79_education_achievement_seed.sql");

        assertThat(sql)
                .contains("'B77-LE-001'")
                .contains("'B77-LE-002'")
                .contains("'B77-LE-003'")
                .contains("'DRAFTING'")
                .contains("'DEPARTMENT_REJECTED'")
                .contains("'EVALUATION_CONFIRMED'")
                .contains("education_achievement_status_histories");
    }

    private String migration(String filename) throws Exception {
        return new ClassPathResource("db/migration/" + filename)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
