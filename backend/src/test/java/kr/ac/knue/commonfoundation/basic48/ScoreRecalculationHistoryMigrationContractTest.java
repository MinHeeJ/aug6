package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class ScoreRecalculationHistoryMigrationContractTest {
    @Test
    void migrationCreatesBizRecalcHistWithB48Seed004ThreeCasesAndReadonlyMenuForReq1545Req1546Req1525Req1526() throws Exception {
        String sql = migrationSql("V52__basic48_score_recalculation_histories.sql");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS biz_recalc_hist")
                .contains("COMMENT ON TABLE biz_recalc_hist")
                .contains("CREATE INDEX IF NOT EXISTS idx_biz_recalc_hist_search")
                .contains("'B48-RECALC-001'")
                .contains("'B48-RECALC-002'")
                .contains("'B48-RECALC-003'")
                .contains("'FORMULA_VERSION_CHANGE'")
                .contains("'TARGET_SCOPE'")
                .contains("'NO_CHANGE'")
                .contains("WHERE seed_id = 'B48-SEED-004'")
                .contains("SCR-SCORE-RECALCULATION-HISTORY")
                .contains("/admin/score-recalculation-histories");
        assertThat(sql)
                .doesNotContain("INSERT INTO roles")
                .doesNotContain("POST /api/business/score-recalculation-histories")
                .doesNotContain("UPDATE biz_score_calc_hist SET")
                .doesNotContain("DELETE FROM biz_score_calc_hist");
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
