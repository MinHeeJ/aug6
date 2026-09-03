package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class ScoreCalculationHistoryMigrationContractTest {
    @Test
    void migrationCreatesBizScoreCalcHistWithB48Seed002ThreeCasesAndReadonlyMenuForReq1510Req1525Req1526() throws Exception {
        String sql = migrationSql("V50__basic48_score_calculation_histories.sql");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS biz_score_calc_hist")
                .contains("COMMENT ON TABLE biz_score_calc_hist")
                .contains("CREATE INDEX IF NOT EXISTS idx_biz_score_calc_hist_search")
                .contains("'B48-CALC-001'")
                .contains("'B48-CALC-002'")
                .contains("'B48-CALC-003'")
                .contains("'SOLE'")
                .contains("'CO_AUTHOR'")
                .contains("'Y'")
                .contains("WHERE seed_id = 'B48-SEED-002'")
                .contains("SCR-SCORE-CALC-HISTORY")
                .contains("/admin/score-calculation-histories");
        assertThat(sql)
                .doesNotContain("INSERT INTO roles")
                .doesNotContain("POST /api/business/score-calculation-histories")
                .doesNotContain("ALTER TABLE score_calculation_generations");
    }

    private String migrationSql(String filename) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
        return Arrays.stream(resources)
                .filter(resource -> filename.equals(resource.getFilename()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(filename + " is missing"))
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
