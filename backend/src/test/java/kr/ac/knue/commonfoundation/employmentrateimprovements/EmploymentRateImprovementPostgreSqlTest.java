package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Opt-in real PostgreSQL/MyBatis checks; absent connection settings skip before any connection or Spring context. */
class EmploymentRateImprovementPostgreSqlTest {
    private Connection connection;
    private SqlSession session;
    private EmploymentRateImprovementMapper mapper;
    private Long ownerId;
    private Long otherId;
    private String organization;
    private Long seedId;

    @BeforeEach
    void setup() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault(
                "EDUCATION_FOUNDATION_POSTGRES_ENABLED", "false")), "PostgreSQL checks are opt-in");
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        Assumptions.assumeTrue(url != null && username != null && password != null,
                "PostgreSQL connection settings are absent");
        connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(false);
        var factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(new SingleConnectionDataSource(connection, true));
        factoryBean.setMapperLocations(new ClassPathResource(
                "mapper/employmentrateimprovements/EmploymentRateImprovementMapper.xml"));
        session = factoryBean.getObject().openSession(connection);
        mapper = session.getMapper(EmploymentRateImprovementMapper.class);
        try (var statement = connection.prepareStatement("""
                SELECT a.achievement_id, a.teacher_user_id, a.organization_code
                FROM education_achievements a
                WHERE a.management_no = 'EDU-SEED-FR-029-001'
                """)) {
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                seedId = rows.getLong(1);
                ownerId = rows.getLong(2);
                organization = rows.getString(3);
            }
        }
        try (var statement = connection.prepareStatement("""
                SELECT a.teacher_user_id
                FROM education_achievements a
                WHERE a.management_no = 'EDU-SEED-FR-029-002'
                """)) {
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                otherId = rows.getLong(1);
            }
        }
    }

    @AfterEach
    void teardown() throws Exception {
        if (connection != null) {
            try {
                connection.rollback();
                if (session != null) {
                    session.close();
                }
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void generatedHeaderKeyPrecedesNullableDetailAndBothHistoriesAndRollback() throws Exception {
        var values = values();
        assertThat(mapper.insertHeader(values)).isEqualTo(1);
        assertThat(values.get("id")).isInstanceOf(Number.class);
        Long id = ((Number) values.get("id")).longValue();
        assertThat(mapper.find(id, false)).isNull(); // Header exists, but no joined detail yet.
        assertThat(mapper.insertDetail(values)).isEqualTo(1);
        assertThat(mapper.insertStatusHistory(values)).isEqualTo(1);
        values.put("before", null);
        values.put("after", "{\"mockExamQuestionPeriod\":\"새 값\"}");
        values.put("changeType", "CREATE");
        assertThat(mapper.insertChangeHistory(values)).isEqualTo(1);
        var saved = mapper.find(id, true);
        assertThat(saved.achievementId()).isEqualTo(id);
        assertThat(saved.specialLectureStartDate()).isNull();
        assertThat(saved.specialLectureEndDate()).isNull();
        assertThat(saved.mockExamQuestionPeriod()).isEqualTo("새 값");
        assertThat(saved.attachmentRef()).isEqualTo("[]");
        try (var statement = connection.prepareStatement("""
                SELECT h.request_id, h.after_value
                FROM data_change_histories h
                WHERE h.target_business = 'education_achievements' AND h.target_key = ?
                """)) {
            statement.setString(1, id.toString());
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("FR029-MAPPER-TEST");
                assertThat(rows.getString(2)).contains("새 값");
            }
        }
        session.rollback();
        assertThat(mapper.find(id, false)).isNull();
    }

    @Test
    void filteredListCountAndDetailShareUnionScopeIncludingOtherOwner() throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO evaluation_organization_mappings (
                    user_id, organization_code, business_type, data_scope, change_reason
                )
                SELECT ?, a.organization_code, 'FACULTY_ACHIEVEMENT', 'DEPARTMENT', 'test scope union'
                FROM education_achievements a
                WHERE a.management_no = 'EDU-SEED-FR-029-002'
                """)) {
            statement.setLong(1, ownerId);
            statement.executeUpdate();
        }
        var criteria = new EmploymentRateImprovementCriteria(
                0, 20, 0, "EDU-SEED-FR-029-002", null, "FR-029", "SUBMITTED");
        assertThat(mapper.list(criteria, ownerId, List.of("R01"))).isEmpty();
        assertThat(mapper.count(criteria, ownerId, List.of("R01"))).isZero();
        var roles = List.of("R01", "R04");
        var rows = mapper.list(criteria, ownerId, roles);
        assertThat(rows).hasSize(1);
        assertThat(mapper.count(criteria, ownerId, roles)).isEqualTo(rows.size());
        assertThat(rows.get(0).teacherUserId()).isEqualTo(otherId);
        assertThat(mapper.countScope(rows.get(0).achievementId(), ownerId, roles)).isEqualTo(1);
        assertThat(mapper.countScope(rows.get(0).achievementId(), ownerId, List.of("R01"))).isZero();
        assertThat(mapper.list(new EmploymentRateImprovementCriteria(
                0, 20, 0, "absent-management-no", null, null, null), ownerId, roles)).isEmpty();
    }

    @Test
    void headerDateUpdateRetainsYearAndConditionalWriteRefusesConfirmedOriginal() throws Exception {
        var values = values();
        values.put("id", seedId);
        values.put("date", LocalDate.parse("2025-12-31"));
        var original = mapper.find(seedId, true);
        assertThat(mapper.updateHeader(values)).isEqualTo(1);
        assertThat(mapper.updateDetail(values)).isEqualTo(1);
        var changed = mapper.find(seedId, false);
        assertThat(changed.evaluationYear()).isEqualTo(original.evaluationYear());
        assertThat(changed.achievementDate()).isEqualTo(LocalDate.parse("2025-12-31"));
        try (var statement = connection.prepareStatement("""
                UPDATE education_achievements
                SET achievement_status = 'EVALUATION_CONFIRMED'
                WHERE achievement_id = ?
                """)) {
            statement.setLong(1, seedId);
            statement.executeUpdate();
        }
        values.put("date", LocalDate.parse("2026-04-15"));
        assertThat(mapper.updateHeader(values)).isZero();
        session.clearCache();
        assertThat(mapper.find(seedId, false).achievementDate()).isEqualTo(LocalDate.parse("2025-12-31"));
    }

    private Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("managementNo", "ERI-DB-" + java.util.UUID.randomUUID());
        values.put("userId", ownerId);
        values.put("organization", organization);
        values.put("year", "2026");
        values.put("code", "FR-029");
        values.put("date", LocalDate.parse("2026-04-10"));
        values.put("start", null);
        values.put("end", null);
        values.put("period", "새 값");
        values.put("attachments", "[]");
        values.put("requestId", "FR029-MAPPER-TEST");
        return values;
    }
}
