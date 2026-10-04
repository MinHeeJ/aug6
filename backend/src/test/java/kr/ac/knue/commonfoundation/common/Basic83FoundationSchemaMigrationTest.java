package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic83FoundationSchemaMigrationTest {
    private final String sql;

    Basic83FoundationSchemaMigrationTest() throws Exception {
        sql = new ClassPathResource("db/migration/V65__basic83_education_achievement_late.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();
    }

    @Test
    void createsTheSharedAchievementLedgerAndAllApprovedDetailAndBatchTables() {
        assertTableWithComment("education_achievements");
        assertTableWithComment("employment_rate_improvement_achievement_details");
        assertTableWithComment("course_operation_achievement_details");
        assertTableWithComment("lecture_improvement_achievement_details");
        assertTableWithComment("employment_rate_batch_jobs");
        assertTableWithComment("employment_rate_batch_job_items");

        assertThat(sql)
                .contains("teacher_user_id bigint not null")
                .contains("organization_code varchar(50) not null")
                .contains("management_item_code varchar(50) not null")
                .contains("achievement_date date not null")
                .contains("achievement_status varchar(30) not null default 'draft'")
                .contains("attachment_ids jsonb not null default '[]'::jsonb")
                .contains("foreign key (achievement_id) references education_achievements(achievement_id)")
                .contains("target_condition_json jsonb not null default '{}'::jsonb")
                .contains("action_type in ('generate', 'delete')")
                .contains("processed_yn char(1) not null default 'n'")
                .contains("unprocessed_reason varchar(500)");
    }

    @Test
    void preservesTheCommonStatusLifecycleAndExtendsExistingHistoryForBasic83Types() {
        assertThat(sql)
                .contains("drop constraint if exists ck_education_achievement_status_histories_type")
                .contains("employment_rate_improvement")
                .contains("course_operation")
                .contains("lecture_improvement")
                .contains("employment_rate")
                .contains("draft:작성중|submitted:제출|department_confirmed:학과장확인")
                .contains("evaluation_confirmed:평가확정|deleted:삭제")
                .contains("deleted_yn char(1) not null default 'n'")
                .contains("create index if not exists idx_education_achievements_search");
    }

    @Test
    void seedsNormalBoundaryAndConfirmedFixturesForEachNewBusinessTableAndMenus() {
        assertThat(sql)
                .contains("취업률 제고 특강")
                .contains("취업률 제고 경계기간 특강")
                .contains("취업률 제고 확정 실적")
                .contains("현장실습 강좌 운영")
                .contains("2025학년도 2학기 강의개선")
                .contains("b83-batch-001")
                .contains("b83-batch-002")
                .contains("b83-batch-003")
                .contains("scr-employment-rate-improvements")
                .contains("scr-course-operations")
                .contains("scr-lecture-improvements")
                .contains("scr-employment-rate-achievements")
                .contains("coalesce((select max(existing_menu.menu_id) from menus existing_menu), 0)")
                .contains("('scr-employment-rate-achievements', 'r07')");
    }

    @Test
    void keepsTheFoundationPostgreSqlSafeAndDoesNotEncodeUndecidedBulkEligibility() {
        assertThat(sql)
                .doesNotContain(" is null or ")
                .doesNotContain("coalesce(:")
                .doesNotContain("? is null")
                .contains("oq-83-01 실행조건이 확정되지 않았습니다.")
                .contains("create index if not exists idx_employment_rate_batch_job_items_job");
    }

    private void assertTableWithComment(String tableName) {
        assertThat(sql)
                .contains("create table if not exists " + tableName)
                .contains("comment on table " + tableName);
    }
}
