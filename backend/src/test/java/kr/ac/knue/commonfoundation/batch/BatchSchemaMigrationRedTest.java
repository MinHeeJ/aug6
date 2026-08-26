package kr.ac.knue.commonfoundation.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class BatchSchemaMigrationRedTest {
    private final String migrationSql;

    BatchSchemaMigrationRedTest() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql");
        migrationSql = Arrays.stream(resources)
                .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                .map(this::readResource)
                .collect(Collectors.joining("\n"))
                .toLowerCase();
    }

    @Test
    void batchDefinitionDependencyAndParameterTablesAreQueryReadyWithDefinitionLinks() {
        assertTableCreatedWithComment("batch_definitions");
        assertTableCreatedWithComment("batch_dependencies");
        assertTableCreatedWithComment("batch_parameters");

        assertThat(migrationSql)
                .contains("batch_id varchar(100) primary key")
                .contains("batch_type varchar(50)")
                .contains("schedule_cycle varchar(100)")
                .contains("max_execution_seconds integer")
                .contains("owner_user_id bigint")
                .contains("foreign key (owner_user_id) references users(user_id)")
                .contains("predecessor_batch_id varchar(100)")
                .contains("successor_batch_id varchar(100)")
                .contains("foreign key (predecessor_batch_id) references batch_definitions(batch_id)")
                .contains("foreign key (successor_batch_id) references batch_definitions(batch_id)")
                .contains("parameter_json jsonb")
                .contains("create index if not exists idx_batch_definitions_search")
                .contains("create index if not exists idx_batch_dependencies_predecessor")
                .contains("create index if not exists idx_batch_parameters_batch_id");
    }

    @Test
    void batchExecutionsTableKeepsStatusReasonOperatorAndOriginalExecutionLink() {
        assertTableCreatedWithComment("batch_executions");

        assertThat(migrationSql)
                .contains("execution_id varchar(100) primary key")
                .contains("batch_id varchar(100) not null")
                .contains("process_type varchar(20) not null")
                .contains("reason varchar(500) not null")
                .contains("operator_user_id bigint not null")
                .contains("execution_status varchar(20) not null")
                .contains("original_execution_id varchar(100)")
                .contains("foreign key (batch_id) references batch_definitions(batch_id)")
                .contains("foreign key (operator_user_id) references users(user_id)")
                .contains("foreign key (original_execution_id) references batch_executions(execution_id)")
                .contains("check (process_type in ('manual_run','stop','rerun'))")
                .contains("check (execution_status in ('waiting','running','stopped','completed','failed'))")
                .contains("comment on column batch_executions.process_type is 'manual_run:수동실행|stop:중지|rerun:재실행'")
                .contains("comment on column batch_executions.execution_status is 'waiting:대기|running:실행중|stopped:중지|completed:완료|failed:실패'")
                .contains("create index if not exists idx_batch_executions_status");
    }

    @Test
    void batchExecutionResultsAndLogsAreReadOnlyQueryReadyByExecutionId() {
        assertTableCreatedWithComment("batch_execution_results");
        assertTableCreatedWithComment("batch_execution_logs");

        assertThat(migrationSql)
                .contains("create table if not exists batch_execution_results")
                .contains("execution_id varchar(100) primary key")
                .contains("started_at timestamp")
                .contains("ended_at timestamp")
                .contains("total_count integer")
                .contains("success_count integer")
                .contains("failure_count integer")
                .contains("excluded_count integer")
                .contains("elapsed_millis bigint")
                .contains("foreign key (execution_id) references batch_executions(execution_id)")
                .contains("create table if not exists batch_execution_logs")
                .contains("log_file_ref varchar(500) not null")
                .contains("create index if not exists idx_batch_execution_results_started_at")
                .contains("create index if not exists idx_batch_execution_logs_execution_id");
    }

    @Test
    void batchRetryTargetsAndResultsPreserveOriginalExecutionConnection() {
        assertTableCreatedWithComment("batch_retry_targets");
        assertTableCreatedWithComment("batch_retry_results");

        assertThat(migrationSql)
                .contains("original_execution_id varchar(100) not null")
                .contains("failed_item_key varchar(200)")
                .contains("retry_execution_id varchar(100) primary key")
                .contains("retry_reason varchar(500) not null")
                .contains("foreign key (original_execution_id) references batch_executions(execution_id)")
                .contains("foreign key (retry_execution_id) references batch_executions(execution_id)")
                .contains("unique (original_execution_id, failed_item_key)")
                .contains("create index if not exists idx_batch_retry_targets_original_execution")
                .contains("create index if not exists idx_batch_retry_results_original_execution");
    }

    private void assertTableCreatedWithComment(String table) {
        assertThat(migrationSql)
                .contains("create table if not exists " + table)
                .contains("comment on table " + table);
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read migration resource: " + resource, exception);
        }
    }
}
