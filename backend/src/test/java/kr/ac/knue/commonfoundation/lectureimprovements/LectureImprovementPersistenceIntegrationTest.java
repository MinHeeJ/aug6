package kr.ac.knue.commonfoundation.lectureimprovements;

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
import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/** Optional PostgreSQL 16 HTTP/materialization/rollback proof in a disposable schema; absent credentials skip. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LectureImprovementPersistenceIntegrationTest {
    private static final String BASE = "/api/business/lecture-improvements";
    private static final String BODY = """
            {"managementItemCode":"FR-031","achievementDate":"2025-04-10",
             "achievementContent":"통합 검증 내용","academicYear":2025,"semester":1}
            """;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private String schema;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LectureImprovementMapper mapper;
    private MockMvc mvc;
    private CurrentUser faculty;

    @BeforeAll
    void isolatedSchema() throws Exception {
        String url = System.getenv("EDUCATION_TEST_DB_URL");
        String username = System.getenv("EDUCATION_TEST_DB_USERNAME");
        String password = System.getenv("EDUCATION_TEST_DB_PASSWORD");
        assumeTrue(url != null && !url.isBlank() && username != null && !username.isBlank() && password != null,
                "Optional PostgreSQL settings absent; runner owns selected-engine verification");
        schema = "lecture_test_" + UUID.randomUUID().toString().replace("-", "");
        dataSource = new DriverManagerDataSource(url, username, password);
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
        }
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load().migrate();
        Properties properties = new Properties();
        properties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(properties);
        jdbc = new JdbcTemplate(dataSource);
        Configuration config = new Configuration(new Environment(
                "lecture-test", new SpringManagedTransactionFactory(), dataSource));
        config.addMapper(EducationAchievementGuardMapper.class);
        try (var stream = new ClassPathResource("mapper/lectureimprovements/LectureImprovementMapper.xml")
                .getInputStream()) {
            new XMLMapperBuilder(stream, config, "LectureImprovementMapper.xml", config.getSqlFragments()).parse();
        }
        var session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config));
        mapper = session.getMapper(LectureImprovementMapper.class);
        var permissions = mock(FunctionPermissionService.class);
        when(permissions.evaluate(any())).thenAnswer(invocation -> {
            var request = (kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest)
                    invocation.getArgument(0);
            return new FunctionPermissionEvaluateResponse(
                    true, request.screenId(), request.roleCode(), request.functionType(), "ALLOW");
        });
        var service = new LectureImprovementService(
                mapper, new EducationAchievementGuardService(session.getMapper(EducationAchievementGuardMapper.class)),
                permissions, json);
        ProxyFactory proxy = new ProxyFactory(service);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        mvc = MockMvcBuilders.standaloneSetup(new LectureImprovementController(
                        (LectureImprovementService) proxy.getProxy()))
                .setControllerAdvice(new GlobalExceptionHandler(), new LectureImprovementExceptionHandler()).build();
        Long owner = jdbc.queryForObject("SELECT user_id FROM users WHERE login_id = 'professor1'", Long.class);
        faculty = new CurrentUser(owner, "professor1", null, "통합 검증 교원", List.of("R01"), List.of());
        // The 2025 production fixture is read-only; only this isolated test schema opens a current input period.
        jdbc.update("""
                INSERT INTO input_period_settings (evaluation_year, area_code, start_at, end_at, change_reason)
                VALUES ('2025', 'EDUCATION', CURRENT_TIMESTAMP - INTERVAL '1 day',
                        CURRENT_TIMESTAMP + INTERVAL '1 day', '격리된 테스트 입력기간')
                """);
    }

    @AfterEach
    void removeFaultInjection() {
        if (jdbc != null) {
            jdbc.execute("DROP TRIGGER IF EXISTS lecture_test_audit_failure ON data_change_histories");
            jdbc.execute("DROP FUNCTION IF EXISTS lecture_test_audit_failure()");
        }
    }

    @AfterAll
    void cleanupOnlyDisposableSchema() {
        if (jdbc != null && schema != null) {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void httpCreateAndUpdateMaterializeDetailAndKeepHeaderYearWithCompleteSnapshots() throws Exception {
        Long id = create();
        mvc.perform(get(BASE + "/" + id).requestAttr("currentUser", faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicYear").value(2025))
                .andExpect(jsonPath("$.data.semester").value(1));
        jdbc.update("""
                INSERT INTO detail_codes (group_id, code_value, code_name, change_reason)
                VALUES ('ACADEMIC_YEAR', '2026', '2026학년도', '격리된 테스트 코드')
                ON CONFLICT (group_id, code_value) DO NOTHING
                """);
        String updated = BODY.replace("2025-04-10", "2026-04-10")
                .replace("\"academicYear\":2025", "\"academicYear\":2026")
                .replace("\"semester\":1", "\"semester\":2")
                .replace("통합 검증 내용", "수정 내용");
        mvc.perform(put(BASE + "/" + id).requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2026))
                .andExpect(jsonPath("$.data.achievement.semester").value(2));
        assertThat(jdbc.queryForObject("""
                SELECT a.achievement_detail ->> 'semester'
                FROM education_achievements a
                WHERE a.achievement_id = ?
                """, String.class, id)).isEqualTo("2");
        assertThat(jdbc.queryForObject("""
                SELECT d.semester_code
                FROM lecture_improvement_achievement_details d
                WHERE d.achievement_id = ?
                """, String.class, id)).isEqualTo("2");
        var audit = jdbc.queryForMap("""
                SELECT h.before_value, h.after_value
                FROM data_change_histories h
                WHERE h.target_business = 'FR-031' AND h.target_key = ? AND h.change_type = 'UPDATE'
                ORDER BY h.history_id DESC
                LIMIT 1
                """, id.toString());
        var before = json.readTree((String) audit.get("before_value"));
        var after = json.readTree((String) audit.get("after_value"));
        assertThat(before.path("detail").path("semester_code").asText()).isEqualTo("1");
        assertThat(after.path("detail").path("semester_code").asText()).isEqualTo("2");
        assertThat(after.path("detail").path("academic_year").asInt()).isEqualTo(2026);
        assertThat(after.path("achievement").path("achievement_detail").path("academicYear").asInt())
                .isEqualTo(after.path("detail").path("academic_year").asInt());
        assertThat(after.path("detail").path("performance_content").asText()).isEqualTo("수정 내용");
        assertThat(after.path("achievement").path("evaluation_year").asText()).isEqualTo("2025");
        assertThat(after.path("achievement").path("achievement_date").asText()).isEqualTo("2026-04-10");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievement_status_histories
                WHERE achievement_type = 'FR-031' AND achievement_id = ?
                """, Long.class, id)).isEqualTo(1);
    }

    @Test
    void scopeUnionAndFilteredListHaveExactlyTheSameCountAndDetailBoundary() {
        jdbc.update("""
                INSERT INTO organization_user_mappings (
                    organization_code, user_id, mapping_type, effective_start_date
                )
                VALUES ('KNUE-DEPT-COMP', ?, 'MANUAL', CURRENT_DATE)
                """, faculty.userId());
        var ownerOnly = new LectureImprovementSearchCriteria(0, 100, 0, null, null, null, null);
        var multi = new CurrentUser(faculty.userId(), faculty.loginId(), null, faculty.name(),
                List.of("R01", "R02", "R04"), List.of());
        var ownerRows = mapper.list(ownerOnly, faculty);
        var unionRows = mapper.list(ownerOnly, multi);
        assertThat(unionRows.size()).isGreaterThan(ownerRows.size());
        assertThat(mapper.count(ownerOnly, multi)).isEqualTo(unionRows.size());
        assertThat(unionRows).containsAll(ownerRows);
        var other = unionRows.stream().filter(row -> !row.teacherUserId().equals(faculty.userId()))
                .findFirst().orElseThrow();
        assertThat(mapper.canRead(other.achievementId(), multi)).isEqualTo(1);
        assertThat(mapper.canRead(other.achievementId(), faculty)).isZero();
        var filter = new LectureImprovementSearchCriteria(0, 100, 0, other.managementNo(), null, null, null);
        assertThat(mapper.list(filter, multi)).hasSize(1);
        assertThat(mapper.count(filter, multi)).isEqualTo(1);
        assertThat(mapper.list(filter, faculty)).isEmpty();
        assertThat(mapper.count(filter, faculty)).isZero();
    }

    @Test
    void confirmedHttpUpdateLeavesEntirePersistedSnapshotUnchanged() throws Exception {
        Long id = create();
        jdbc.update("UPDATE education_achievements SET achievement_status = 'EVALUATION_CONFIRMED'"
                + " WHERE achievement_id = ?", id);
        String original = mapper.snapshot(id);
        long audits = count("data_change_histories");
        mvc.perform(put(BASE + "/" + id).requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("내용", "변경")))
                .andExpect(status().isConflict());
        assertThat(mapper.snapshot(id)).isEqualTo(original);
        assertThat(count("data_change_histories")).isEqualTo(audits);
    }

    @Test
    void auditFailureRollsBackHeaderDetailAndStatusAndUpdateOldValues() throws Exception {
        Long id = create();
        String original = mapper.snapshot(id);
        long headers = count("education_achievements");
        long details = count("lecture_improvement_achievement_details");
        long statuses = count("education_achievement_status_histories");
        long audits = count("data_change_histories");
        jdbc.execute("""
                CREATE FUNCTION lecture_test_audit_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.target_business = 'FR-031' THEN
                        RAISE EXCEPTION 'isolated audit failure';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER lecture_test_audit_failure BEFORE INSERT ON data_change_histories
                FOR EACH ROW EXECUTE FUNCTION lecture_test_audit_failure()
                """);
        mvc.perform(post(BASE).requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError());
        mvc.perform(put(BASE + "/" + id).requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("내용", "변경")))
                .andExpect(status().isInternalServerError());
        assertThat(count("education_achievements")).isEqualTo(headers);
        assertThat(count("lecture_improvement_achievement_details")).isEqualTo(details);
        assertThat(count("education_achievement_status_histories")).isEqualTo(statuses);
        assertThat(count("data_change_histories")).isEqualTo(audits);
        assertThat(mapper.snapshot(id)).isEqualTo(original);
    }

    private Long create() throws Exception {
        String response = mvc.perform(post(BASE).requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("achievement").path("achievementId").asLong();
    }

    private long count(String table) {
        // Only file-local constant table names are used, never untrusted identifiers.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
