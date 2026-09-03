package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class ScoreAdjustmentHistoryMigrationContractTest {
    @Test
    void migrationCreatesBizScoreAdjHistWithB48Seed003ThreeCasesAndReadonlyMenuForReq1541Req1542Req1525Req1526() throws Exception {
        String sql = migrationSql("V51__basic48_score_adjustment_histories.sql");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS biz_score_adj_hist")
                .contains("COMMENT ON TABLE biz_score_adj_hist")
                .contains("CREATE INDEX IF NOT EXISTS idx_biz_score_adj_hist_search")
                .contains("'B48-ADJ-001'")
                .contains("'B48-ADJ-002'")
                .contains("'B48-ADJ-003'")
                .contains("'SCORE'")
                .contains("'PERCENTAGE'")
                .contains("우수 학술지 가점 반영")
                .contains("중복 인정 제외")
                .contains("평가백분율 조정")
                .contains("WHERE seed_id = 'B48-SEED-003'")
                .contains("SCR-SCORE-ADJUSTMENT-HISTORY")
                .contains("/admin/score-adjustment-histories");
        assertThat(sql)
                .doesNotContain("INSERT INTO roles")
                .doesNotContain("POST /api/business/score-adjustment-histories")
                .doesNotContain("ALTER TABLE biz_score_calc_hist");
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
