package kr.ac.knue.commonfoundation.basic45;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic45FoundationSchemaMigrationTest {
    private final String migrationSql;

    Basic45FoundationSchemaMigrationTest() throws Exception {
        migrationSql = new ClassPathResource("db/migration/V46__basic45_evaluation_batch_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void migrationAddsEvaluationBatchFoundationTables() {
        assertThat(migrationSql)
                .contains("create table if not exists evaluation_materials")
                .contains("create table if not exists evaluation_batch_requests")
                .contains("create table if not exists evaluation_batch_results")
                .contains("create table if not exists score_calculation_generations")
                .contains("create table if not exists final_evaluation_confirmations");
    }

    @Test
    void migrationDeclaresCommentsIndexesAndSeedFixtures() {
        assertThat(migrationSql)
                .contains("comment on table evaluation_materials")
                .contains("comment on column evaluation_materials.material_status")
                .contains("comment on column evaluation_batch_requests.request_status")
                .contains("comment on column evaluation_batch_results.error_detail_json")
                .contains("create index if not exists idx_evaluation_materials_generation")
                .contains("create index if not exists idx_evaluation_batch_results_batch")
                .contains("b45-seed-001")
                .contains("b45-seed-002")
                .contains("b45-seed-003")
                .contains("b45-seed-004")
                .contains("b45-seed-005");
    }
}
