package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AchievementSearchSqlTest {
    @Test
    void closesAccessScopePredicateBeforeLectureOrderBy() {
        String sql = LectureAchievementSql.list(Map.of(
                "criteria",
                new LectureAchievementModels.SearchCriteria(
                        0,
                        20,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        ));

        assertThat(sql).contains("mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL'))))")
                .contains("ORDER BY achievement.occurred_date DESC");
    }

    @Test
    void closesAccessScopePredicateBeforeLectureEvaluationOrderBy() {
        String sql = LectureEvaluationAchievementSql.list(Map.of(
                "criteria",
                new LectureEvaluationAchievementModels.SearchCriteria(
                        0,
                        20,
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 1, 1),
                        null,
                        null
                )
        ));

        assertThat(sql).contains("mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL'))))")
                .contains("AND achievement.occurred_date >= #{criteria.occurredDateFrom}");
    }

    @Test
    void closesAccessScopePredicateBeforeDegreeCompletionOrderBy() {
        String sql = DegreeCompletionAchievementSql.list(Map.of(
                "criteria",
                new DegreeCompletionAchievementModels.SearchCriteria(0, 20, null, null, null)
        ));

        assertThat(sql).contains("mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL'))))")
                .contains("ORDER BY achievement.occurred_date DESC");
    }
}
