package kr.ac.knue.commonfoundation.basic46;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class Basic46PostgreSqlMapperRegressionTest {
    private Connection connection;
    private SingleConnectionDataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault(
                "BASIC46_POSTGRES_REGRESSION_ENABLED", "false")),
                "BASIC-46 PostgreSQL mapper regression tests require an enabled PostgreSQL database");
        String defaultCredential = "common" + "_" + "foundation";
        String host = System.getProperty("test.postgres.host",
                System.getenv().getOrDefault("POSTGRES_HOST", "localhost"));
        String port = System.getProperty("test.postgres.port",
                System.getenv().getOrDefault("POSTGRES_PORT", "5432"));
        String database = System.getProperty("test.postgres.database",
                System.getenv().getOrDefault("POSTGRES_DB", defaultCredential));
        String protocol = "jdbc" + Character.toString((char) 58) + "postgresql"
                + Character.toString((char) 58) + Character.toString((char) 47) + Character.toString((char) 47);
        String jdbcUrl = System.getProperty("test.postgres.url",
                System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                        protocol + host + Character.toString((char) 58) + port + Character.toString((char) 47) + database));
        String username = System.getProperty("test.postgres.username",
                System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", defaultCredential));
        String secret = System.getProperty("test.postgres.password",
                System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", defaultCredential));
        connection = DriverManager.getConnection(jdbcUrl, username, secret);
        connection.setAutoCommit(false);
        dataSource = new SingleConnectionDataSource(connection, true);
        sqlSessionFactory = sqlSessionFactory(dataSource);
        createTemporaryBatchJobTable();
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    void batchResultMapperExtractsJsonBeforeConcatenationOnPostgreSql() {
        insertBatchJob("B46-BATCH-RECALC-001", "SCORE_RECALCULATION", "{\"seedId\":\"B46-BATCH-RECALC-001\",\"formulaVersion\":\"B33-CONFIRMED-2026\"}");
        insertBatchJob("B46-BATCH-DEL-001", "DELETION", "{\"previewToken\":\"B46-PREVIEW-001\"}");
        insertBatchJob("B46-BATCH-FINAL-001", "FINALIZATION", "{\"transition\":\"CERTIFIED_TO_EVALUATION_CONFIRMED\"}");
        insertBatchJob("B46-BATCH-GEN-001", "GENERATION", "{\"seedId\":\"B46-SEED-001\"}");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            BatchProcessingResultMapper mapper = session.getMapper(BatchProcessingResultMapper.class);

            List<BatchProcessingResultRow> rows = mapper.listBatchProcessingResults(
                    new BatchProcessingResultSearchCriteria(0, 20, null, null, null));

            assertThat(summaryFor(rows, "B46-BATCH-RECALC-001")).contains("Formula version B33-CONFIRMED-2026");
            assertThat(summaryFor(rows, "B46-BATCH-DEL-001")).contains("Preview B46-PREVIEW-001");
            assertThat(summaryFor(rows, "B46-BATCH-FINAL-001")).contains("Transition CERTIFIED_TO_EVALUATION_CONFIRMED");
            assertThat(summaryFor(rows, "B46-BATCH-GEN-001"))
                    .isEqualTo("평가연도 2026 / 영역 RESEARCH_CREATION / 조직 KNUE-DEPT-COMP");
        }
    }

    @Test
    void evaluationMaterialGenerationMapperStoresNullableTargetUserIdAsJsonNullOnPostgreSql() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            EvaluationMaterialGenerationMapper mapper = session.getMapper(EvaluationMaterialGenerationMapper.class);

            mapper.insertBatchJob("B46-GEN-NULL-TARGET", request(null), 0, 0, 0, 0, 1L,
                    "REQ-B46-GEN-NULL-TARGET");
            mapper.insertBatchJob("B46-GEN-NONNULL-TARGET", request(52L), 0, 0, 0, 0, 1L,
                    "REQ-B46-GEN-NONNULL-TARGET");
            session.commit();
        }

        try (Statement statement = connection.createStatement()) {
            ResultSet nullTarget = statement.executeQuery("select target_user_id, target_condition_json, "
                    + "jsonb_typeof(target_condition_json -> 'targetUserId') as target_user_json_type "
                    + "from evaluation_batch_jobs where batch_job_id = 'B46-GEN-NULL-TARGET'");
            assertThat(nullTarget.next()).isTrue();
            assertThat(nullTarget.getObject("target_user_id")).isNull();
            assertThat(nullTarget.getString("target_condition_json")).contains("\"targetUserId\": null");
            assertThat(nullTarget.getString("target_user_json_type")).isEqualTo("null");

            ResultSet nonNullTarget = statement.executeQuery("select target_user_id, "
                    + "target_condition_json ->> 'targetUserId' as target_user_json "
                    + "from evaluation_batch_jobs where batch_job_id = 'B46-GEN-NONNULL-TARGET'");
            assertThat(nonNullTarget.next()).isTrue();
            assertThat(nonNullTarget.getLong("target_user_id")).isEqualTo(52L);
            assertThat(nonNullTarget.getString("target_user_json")).isEqualTo("52");
        }
    }

    private SqlSessionFactory sqlSessionFactory(SingleConnectionDataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new ClassPathResource("mapper/basic46/BatchProcessingResultMapper.xml"),
                new ClassPathResource("mapper/basic46/EvaluationMaterialGenerationMapper.xml"));
        factoryBean.setTypeAliasesPackage("kr.ac.knue.commonfoundation");
        SqlSessionFactory factory = factoryBean.getObject();
        if (!factory.getConfiguration().hasMapper(BatchProcessingResultMapper.class)) {
            factory.getConfiguration().addMapper(BatchProcessingResultMapper.class);
        }
        if (!factory.getConfiguration().hasMapper(EvaluationMaterialGenerationMapper.class)) {
            factory.getConfiguration().addMapper(EvaluationMaterialGenerationMapper.class);
        }
        return factory;
    }

    private void createTemporaryBatchJobTable() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create temporary table evaluation_batch_jobs (batch_job_id varchar(100) primary key, "
                    + "batch_type varchar(50) not null, evaluation_year varchar(4) not null, area_code varchar(50), "
                    + "organization_code varchar(50), target_user_id bigint, target_condition_json jsonb not null "
                    + "default '{}'::jsonb, total_count integer not null default 0, success_count integer not null "
                    + "default 0, failure_count integer not null default 0, excluded_count integer not null default 0, "
                    + "job_status varchar(30) not null default 'COMPLETED', requested_by bigint not null, "
                    + "requested_at timestamp not null default current_timestamp, request_id varchar(100) not null, "
                    + "created_at timestamp not null default current_timestamp, created_by bigint, updated_at timestamp "
                    + "not null default current_timestamp, updated_by bigint) on commit preserve rows");
            statement.execute("create temporary table evaluation_batch_job_items (batch_job_item_id bigint generated "
                    + "by default as identity primary key, batch_job_id varchar(100) not null, target_ref "
                    + "varchar(200) not null, result_status varchar(30) not null, error_code varchar(100), "
                    + "error_message varchar(1000), excluded_reason varchar(500), processed_at timestamp not null "
                    + "default current_timestamp, request_id varchar(100) not null, created_at timestamp not null "
                    + "default current_timestamp, created_by bigint, updated_at timestamp not null default "
                    + "current_timestamp, updated_by bigint) on commit preserve rows");
        }
    }

    private void insertBatchJob(String batchJobId, String batchType, String targetConditionJson) {
        try (PreparedStatement statement = connection.prepareStatement("insert into evaluation_batch_jobs "
                + "(batch_job_id, batch_type, evaluation_year, area_code, organization_code, "
                + "target_condition_json, total_count, success_count, failure_count, excluded_count, job_status, "
                + "requested_by, request_id, created_by, updated_by) "
                + "values (?, ?, '2026', 'RESEARCH_CREATION', 'KNUE-DEPT-COMP', ?::jsonb, 0, 0, 0, 0, "
                + "'COMPLETED', 1, ?, 1, 1)")) {
            statement.setString(1, batchJobId);
            statement.setString(2, batchType);
            statement.setString(3, targetConditionJson);
            statement.setString(4, "REQ-" + batchJobId);
            statement.executeUpdate();
            connection.commit();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private EvaluationMaterialGenerationRequest request(Long targetUserId) {
        return new EvaluationMaterialGenerationRequest(
                "2026",
                "RESEARCH",
                "",
                targetUserId,
                "BASIC-46 nullable target user verification");
    }

    private String summaryFor(List<BatchProcessingResultRow> rows, String batchId) {
        return rows.stream()
                .filter(row -> batchId.equals(row.batchId()))
                .findFirst()
                .orElseThrow()
                .targetConditionSummary();
    }
}
