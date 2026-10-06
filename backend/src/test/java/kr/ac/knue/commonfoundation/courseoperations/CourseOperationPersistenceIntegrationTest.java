package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.apache.ibatis.session.Configuration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Optional PostgreSQL 16 HTTP-to-MyBatis checks, including actual transaction rollback and record materialization.
 * Session/menu registration belongs to T016; this test supplies a principal, not a substitute production filter.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CourseOperationPersistenceIntegrationTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private JdbcTemplate admin;
    private JdbcTemplate db;
    private String schema;
    private MockMvc mvc;
    private Long owner;

    @BeforeAll
    void setupOnlyWithRunnerDatabase() throws Exception {
        String url = System.getenv("EDUCATION_TEST_DB_URL");
        String username = System.getenv("EDUCATION_TEST_DB_USERNAME");
        String password = System.getenv("EDUCATION_TEST_DB_PASSWORD");
        assumeTrue(url != null && !url.isBlank() && username != null && !username.isBlank() && password != null,
                "Optional PostgreSQL settings absent; runner owns SQL verification");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        schema = "course_test_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(url, username, password).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").cleanDisabled(true).load().migrate();
        DriverManagerDataSource source = new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, username, password);
        db = new JdbcTemplate(source);
        try (var connection = source.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
        }
        owner = db.queryForObject("SELECT user_id FROM users WHERE login_id = 'professor1'", Long.class);
        db.update("DELETE FROM evaluation_finalizations WHERE target_user_id = ?", owner);
        db.update("""
                INSERT INTO input_period_settings (evaluation_year, area_code, start_at, end_at)
                VALUES ('2025', 'EDUCATION', CURRENT_TIMESTAMP - INTERVAL '1 day',
                        CURRENT_TIMESTAMP + INTERVAL '1 day')
                """);
        db.update("""
                INSERT INTO evaluation_date_settings (evaluation_year, area_code, start_at, end_at)
                VALUES ('2025', 'EDUCATION', TIMESTAMP '2025-01-01', TIMESTAMP '2025-12-31 23:59:59')
                """);
        Configuration configuration = new Configuration();
        configuration.addMapper(EducationAchievementGuardMapper.class);
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/courseoperations/CourseOperationMapper.xml"));
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        FunctionPermissionService functions = mock(FunctionPermissionService.class);
        when(functions.evaluate(any())).thenReturn(new FunctionPermissionEvaluateResponse(
                true, "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT", "R01", "CREATE", "ALLOW"));
        CourseOperationService target = new CourseOperationService(
                session.getMapper(CourseOperationMapper.class),
                new EducationAchievementGuardService(session.getMapper(EducationAchievementGuardMapper.class)),
                functions, json);
        TransactionInterceptor transactions = new TransactionInterceptor();
        transactions.setTransactionManager(new DataSourceTransactionManager(source));
        transactions.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(transactions);
        mvc = MockMvcBuilders.standaloneSetup(new CourseOperationController((CourseOperationService) proxy.getProxy()))
                .setControllerAdvice(new CourseOperationExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    @AfterAll
    void cleanupOnlyTheIsolatedSchema() {
        if (admin != null && schema != null) {
            admin.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void persistedCrudScopesSnapshotsAndRollbackUseTheRealMapper() throws Exception {
        CurrentUser teacher = principal(List.of("R01"));
        String result = mvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", teacher).header("X-Request-Id", "course-db-create")
                        .contentType(MediaType.APPLICATION_JSON).content(body("2025-05-03", "등록 내역")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.performanceDetails").value("등록 내역"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(result).path("data").path("achievement").path("achievementId").asLong();
        assertThat(id).isPositive();
        assertThat(db.queryForObject("""
                SELECT COUNT(*) FROM education_achievement_status_histories
                WHERE achievement_type = 'FR-030' AND achievement_id = ? AND next_status = 'DRAFT'
                """, Long.class, id)).isEqualTo(1);
        assertThat(db.queryForObject("""
                SELECT COUNT(*) FROM data_change_histories
                WHERE target_business = 'FR-030' AND target_key = ? AND request_id = 'course-db-create'
                """, Long.class, String.valueOf(id))).isEqualTo(1);
        mvc.perform(put("/api/business/course-operations/" + id).requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "course-db-update").contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-01-01", "수정 내역")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"));
        mvc.perform(get("/api/business/course-operations/" + id).requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.performanceDetails").value("수정 내역"));
        String snapshot = db.queryForObject("""
                SELECT after_value FROM data_change_histories WHERE request_id = 'course-db-update'
                """, String.class);
        assertThat(json.readTree(snapshot).path("achievement").path("evaluation_year").asText()).isEqualTo("2025");
        assertThat(json.readTree(snapshot).path("achievement").path("achievement_detail")
                .path("performanceDetails").asText()).isEqualTo("수정 내역");
        assertThat(json.readTree(snapshot).path("detail").path("performance_detail").asText()).isEqualTo("수정 내역");
        assertThat(db.queryForObject("""
                SELECT before_value FROM data_change_histories WHERE request_id = 'course-db-update'
                """, String.class)).contains("등록 내역", "2025-05-03");

        CourseOperationMapper actual = mapperForAssertions();
        // The shared education ledger must not let FR-030 consume another feature's management item.
        assertThat(actual.managementRules("FR-031", "2025")).isEmpty();
        long headersBeforeWrongItem = db.queryForObject("SELECT COUNT(*) FROM education_achievements", Long.class);
        mvc.perform(post("/api/business/course-operations").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-05-03", "다른 유형 항목").replace("FR-030", "FR-031")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
        assertThat(db.queryForObject("SELECT COUNT(*) FROM education_achievements", Long.class))
                .isEqualTo(headersBeforeWrongItem);

        // All seeded three states are reachable to department scope, while owner scope is narrower.
        CourseOperationSearchCriteria criteria = new CourseOperationSearchCriteria(
                0, 20, 0, null, null, "FR-030", null);
        assertThat(actual.list(criteria, owner, List.of("R01")))
                .hasSize((int) actual.count(criteria, owner, List.of("R01")));
        assertThat(actual.list(criteria, owner, List.of("R02")))
                .hasSize((int) actual.count(criteria, owner, List.of("R02")));
        long scoped = actual.count(criteria, owner, List.of("R02"));
        assertThat(scoped).isGreaterThanOrEqualTo(actual.count(criteria, owner, List.of("R01")));
        assertThat(actual.count(criteria, owner, List.of("R01", "R02"))).isEqualTo(scoped);
        CourseOperationSearchCriteria filtered = new CourseOperationSearchCriteria(
                0, 20, 0, null, null, "FR-030", "DRAFT");
        assertThat(actual.list(filtered, owner, List.of("R02")))
                .hasSize((int) actual.count(filtered, owner, List.of("R02")))
                .allMatch(row -> row.achievementStatus().equals("DRAFT"));
        Long otherId = db.queryForObject("""
                SELECT achievement_id FROM education_achievements WHERE management_no = 'EDU-FR-030-2025-002'
                """, Long.class);
        mvc.perform(get("/api/business/course-operations/" + otherId).requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/business/course-operations/" + otherId)
                        .requestAttr("currentUser", principal(List.of("R01", "R02"))))
                .andExpect(status().isOk());

        // A real database error after header/detail updates proves the production @Transactional rollback.
        db.execute("""
                CREATE FUNCTION reject_course_history() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.request_id = 'course-db-fail' THEN
                        RAISE EXCEPTION 'integration test audit rejection';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        db.execute("""
                CREATE TRIGGER reject_course_history BEFORE INSERT ON data_change_histories
                FOR EACH ROW EXECUTE FUNCTION reject_course_history()
                """);
        mvc.perform(put("/api/business/course-operations/" + id).requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "course-db-fail").contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-06-01", "롤백할 내역")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message")
                        .value("오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요."));
        assertThat(actual.find(id).performanceDetails()).isEqualTo("수정 내역");
        assertThat(actual.find(id).achievementDate().toString()).isEqualTo("2026-01-01");
        assertThat(actual.snapshot(id)).contains("수정 내역").doesNotContain("롤백할 내역");
        long headersBefore = db.queryForObject("SELECT COUNT(*) FROM education_achievements", Long.class);
        long detailsBefore = db.queryForObject("SELECT COUNT(*) FROM course_operation_achievement_details", Long.class);
        long statusBefore = db.queryForObject(
                "SELECT COUNT(*) FROM education_achievement_status_histories", Long.class);
        mvc.perform(post("/api/business/course-operations").requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "course-db-fail").contentType(MediaType.APPLICATION_JSON)
                        .content(body("2025-07-01", "등록 롤백")))
                .andExpect(status().isInternalServerError());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM education_achievements", Long.class))
                .isEqualTo(headersBefore);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM course_operation_achievement_details", Long.class))
                .isEqualTo(detailsBefore);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM education_achievement_status_histories", Long.class))
                .isEqualTo(statusBefore);
        db.update("""
                UPDATE education_achievements
                SET achievement_status = 'EVALUATION_CONFIRMED'
                WHERE achievement_id = ?
                """, id);
        mvc.perform(put("/api/business/course-operations/" + id).requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body("2025-06-01", "금지한 변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        assertThat(actual.find(id).performanceDetails()).isEqualTo("수정 내역");
    }

    private CourseOperationMapper mapperForAssertions() throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(db.getDataSource());
        factory.setMapperLocations(new ClassPathResource("mapper/courseoperations/CourseOperationMapper.xml"));
        return new SqlSessionTemplate(factory.getObject()).getMapper(CourseOperationMapper.class);
    }

    private CurrentUser principal(List<String> roles) {
        return new CurrentUser(owner, "professor1", null, "교원", roles, List.of());
    }

    private String body(String date, String details) throws Exception {
        return json.writeValueAsString(java.util.Map.of("managementItemCode", "FR-030", "achievementDate", date,
                "performanceDetails", details, "attachmentIds", List.of()));
    }
}
