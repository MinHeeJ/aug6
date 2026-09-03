package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class EvaluationSnapshotMigrationContractTest {
    @Test
    void migrationCreatesBizEvalSnapshotsWithB48Seed001ThreeCasesAndReadonlyMenuForReq1503Req1525Req1526() throws Exception {
        String sql = migrationSql("V49__basic48_evaluation_snapshots.sql");

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS biz_eval_snapshots")
                .contains("COMMENT ON TABLE biz_eval_snapshots")
                .contains("CREATE INDEX IF NOT EXISTS idx_biz_eval_snapshots_search")
                .contains("'B48-SNAPSHOT-001'")
                .contains("'B48-SNAPSHOT-002'")
                .contains("'B48-SNAPSHOT-003'")
                .contains("'2026-FINAL-01'")
                .contains("'2026-FINAL-RECONFIRM-01'")
                .contains("'2025-FINAL-01'")
                .contains("WHERE seed_id = 'B48-SEED-001'")
                .contains("SCR-EVAL-SNAPSHOT-HISTORY")
                .contains("/admin/evaluation-snapshots");
        assertThat(sql)
                .doesNotContain("CREATE TABLE IF NOT EXISTS roles")
                .doesNotContain("ALTER TABLE evaluation_materials")
                .doesNotContain("INSERT INTO roles");
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
