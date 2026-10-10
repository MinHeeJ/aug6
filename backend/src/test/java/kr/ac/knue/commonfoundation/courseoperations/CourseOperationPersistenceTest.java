package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Opt-in PostgreSQL mapper materialization and write/readback checks, rolled back after each test. */
class CourseOperationPersistenceTest {
    private Connection connection;
    private SqlSession session;
    private CourseOperationMapper mapper;
    private Long facultyId;

    @BeforeEach
    void connectToExplicitlyProvidedTestDatabase() throws Exception {
        String url = System.getenv("EDUCATION_ACHIEVEMENT_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL persistence test database not provided");
        connection = DriverManager.getConnection(url,
                System.getenv().getOrDefault("EDUCATION_ACHIEVEMENT_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("EDUCATION_ACHIEVEMENT_TEST_DB_PASSWORD", ""));
        connection.setAutoCommit(false);
        Configuration configuration = new Configuration();
        try (var stream = new ClassPathResource("mapper/courseoperations/CourseOperationMapper.xml").getInputStream()) {
            new XMLMapperBuilder(stream, configuration, "CourseOperationMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(connection);
        mapper = session.getMapper(CourseOperationMapper.class);
        try (var statement = connection.prepareStatement("SELECT user_id FROM users WHERE login_id = 'professor1'")) {
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                facultyId = rows.getLong(1);
            }
        }
    }

    @AfterEach
    void rollbackAndClose() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                if (session != null) session.close();
                connection.close();
            }
        }
    }

    @Test
    void headerGeneratedKeyPrecedesDetailAndMapsNullAttachments() {
        Map<String, Object> header = header();
        mapper.insertHeader(header);
        Long id = ((Number) header.get("achievementId")).longValue();
        assertThat(id).isPositive();
        mapper.insertDetail(id, "개설 강좌 운영");
        CourseOperationRow row = mapper.find(id, false);
        assertThat(row.performanceDetails()).isEqualTo("개설 강좌 운영");
        assertThat(row.teacherUserId()).isEqualTo(facultyId);
        assertThat(row.attachmentRef()).isNull();
        assertThat(row.attachmentIds()).isEmpty();
        assertThat(row.createdAt()).isNotNull();
        assertThat(row.createdBy()).isEqualTo(facultyId);
    }

    @Test
    void listAndCountUseTheSameFiltersAndUnionScope() {
        CourseOperationSearchCriteria filtered = criteria("CO-002");
        List<String> union = List.of("R01", "R02");
        List<CourseOperationRow> rows = mapper.list(filtered, facultyId, union);
        assertThat(rows).hasSize(1);
        assertThat(mapper.count(filtered, facultyId, union)).isEqualTo(rows.size());
        assertThat(rows.get(0).managementNo()).isEqualTo("CO-002");
        assertThat(mapper.countScope(rows.get(0).achievementId(), facultyId, union)).isEqualTo(1);
        assertThat(mapper.count(filtered, facultyId, List.of("R01"))).isZero();
        assertThat(mapper.countScope(rows.get(0).achievementId(), facultyId, List.of("R01"))).isZero();
    }

    @Test
    void absentAndNullFiltersAreTypedAndReturnZeroOrScopedRows() {
        CourseOperationSearchCriteria criteria = criteria(null);
        List<CourseOperationRow> rows = mapper.list(criteria, facultyId, List.of("R01"));
        assertThat(rows).allMatch(row -> row.teacherUserId().equals(facultyId));
        assertThat(mapper.count(criteria, facultyId, List.of("R01"))).isEqualTo(rows.size());
        assertThat(mapper.list(criteria("missing-" + UUID.randomUUID()), facultyId, List.of("R09"))).isEmpty();
    }

    @Test
    void updatePreservesParentYearAndAuditsOldAndNewDetail() {
        Map<String, Object> header = header();
        mapper.insertHeader(header);
        Long id = ((Number) header.get("achievementId")).longValue();
        mapper.insertDetail(id, "원본");
        CourseOperationRow old = mapper.find(id, true);
        CourseOperationRequest body = new CourseOperationRequest("COURSE_OPERATION", LocalDate.of(2025, 12, 31),
                "수정", null, null, List.of(), null);
        assertThat(mapper.updateHeader(id, body, "DRAFT", null, facultyId)).isEqualTo(1);
        mapper.updateDetail(id, "수정");
        mapper.insertChangeHistory(id, "UPDATE", "원본", "수정", facultyId, "course-persistence-request");
        session.clearCache();
        CourseOperationRow saved = mapper.find(id, false);
        assertThat(saved.evaluationYear()).isEqualTo(old.evaluationYear());
        assertThat(saved.achievementDate()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(saved.performanceDetails()).isEqualTo("수정");
        assertThat(saved.createdAt()).isEqualTo(old.createdAt());
    }

    @Test
    void confirmedHeaderConditionalWriteDoesNotChangeOriginal() {
        CourseOperationRow confirmed = mapper.list(criteria("CO-003"), facultyId, List.of("R09")).get(0);
        CourseOperationRequest body = new CourseOperationRequest("COURSE_OPERATION", LocalDate.of(2026, 4, 10),
                "금지 변경", null, null, List.of(), null);
        assertThat(mapper.updateHeader(confirmed.achievementId(), body, "DRAFT", null, facultyId)).isZero();
        assertThat(mapper.find(confirmed.achievementId(), false)).isEqualTo(confirmed);
    }

    private Map<String, Object> header() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("managementNo", "CO-" + UUID.randomUUID());
        values.put("teacherUserId", facultyId);
        values.put("organizationCode", mapper.findOrganization(facultyId));
        values.put("evaluationYear", "2026");
        values.put("managementItemCode", "COURSE_OPERATION");
        values.put("achievementDate", LocalDate.of(2026, 4, 10));
        values.put("attachments", null);
        values.put("userId", facultyId);
        return values;
    }

    private CourseOperationSearchCriteria criteria(String managementNo) {
        return new CourseOperationSearchCriteria(0, 100, 0L, managementNo, null, null, null);
    }
}
