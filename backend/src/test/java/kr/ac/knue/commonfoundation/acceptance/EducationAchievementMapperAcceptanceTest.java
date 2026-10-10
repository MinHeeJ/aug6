package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.DriverManager;
import java.util.*;
import kr.ac.knue.commonfoundation.courseoperations.CourseOperationSearchCriteria;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementSearch;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementSearchCriteria;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Merged MyBatis registration checks plus explicitly opt-in PostgreSQL materialization/filtering. */
class EducationAchievementMapperAcceptanceTest {
    private static final Map<String, String> MAPPERS = Map.of(
            "employmentrateimprovements", "EmploymentRateImprovementMapper",
            "courseoperations", "CourseOperationMapper",
            "lectureimprovements", "LectureImprovementMapper",
            "employmentrateachievements", "EmploymentRateAchievementMapper");

    private Configuration configuration() throws Exception {
        Configuration configuration = new Configuration();
        for (var entry : MAPPERS.entrySet()) {
            String resource = "mapper/" + entry.getKey() + "/" + entry.getValue() + ".xml";
            try (var stream = new ClassPathResource(resource).getInputStream()) {
                new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        return configuration;
    }

    private String namespace(String feature) {
        return "kr.ac.knue.commonfoundation." + feature + "." + MAPPERS.get(feature);
    }

    private Map<String, Object> query(String feature, Long userId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        parameters.put("roles", List.of("R09"));
        switch (feature) {
            case "employmentrateimprovements" -> parameters.put("criteria",
                    new EmploymentRateImprovementSearch(0, 100, 0, null, null, null, null));
            case "courseoperations" -> parameters.put("criteria",
                    new CourseOperationSearchCriteria(0, 100, 0L, null, null, null, null));
            case "lectureimprovements" -> parameters.put("criteria",
                    new LectureImprovementSearchCriteria(0, 100, 0, null, null, null, null, null));
            default -> {
                parameters.put("pageSize", 100);
                parameters.put("pageOffset", 0L);
            }
        }
        return parameters;
    }

    @Test
    void allMergedXmlMappersRegisterTogetherWithResolvableRecordConstructors() throws Exception {
        Configuration configuration = configuration();
        for (var entry : MAPPERS.entrySet()) {
            String namespace = namespace(entry.getKey());
            assertThat(configuration.hasMapper(Class.forName(namespace))).isTrue();
            for (String statement : List.of("list", "count", "find")) {
                assertThat(configuration.hasStatement(namespace + "." + statement)).isTrue();
            }
            for (var result : configuration.getMappedStatement(namespace + ".list").getResultMaps()) {
                if (!result.getType().isRecord()) continue;
                Class<?>[] types = result.getConstructorResultMappings().stream()
                        .map(mapping -> mapping.getJavaType()).toArray(Class<?>[]::new);
                assertThat(result.getType().getDeclaredConstructor(types)).isNotNull();
            }
        }
    }

    @Test
    void absentOptionalFiltersNeverBecomeNullBoundPredicates() throws Exception {
        Configuration configuration = configuration();
        for (String feature : MAPPERS.keySet()) {
            for (String operation : List.of("list", "count")) {
                var bound = configuration.getMappedStatement(namespace(feature) + "." + operation)
                        .getBoundSql(query(feature, 101L));
                assertThat(bound.getParameterMappings().stream().map(ParameterMapping::getProperty))
                        .noneMatch(name -> name.contains("managementNo") || name.contains("teacherName")
                                || name.contains("managementItemCode") || name.contains("achievementStatus")
                                || name.contains("evaluationYear"));
            }
        }
    }

    @Test
    void postgresqlMaterializesEachTypeAndHidesSoftDeletedRowsWithoutPhysicalDeletion() throws Exception {
        String url = System.getenv("EDUCATION_ACHIEVEMENT_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL persistence test database not provided");
        // Same opt-in connection and rollback lifecycle as CourseOperationPersistenceTest.
        try (var connection = DriverManager.getConnection(url,
                System.getenv().getOrDefault("EDUCATION_ACHIEVEMENT_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("EDUCATION_ACHIEVEMENT_TEST_DB_PASSWORD", ""))) {
            connection.setAutoCommit(false);
            try (var session = new SqlSessionFactoryBuilder().build(configuration()).openSession(connection)) {
                Long faculty;
                try (var statement = connection.prepareStatement("SELECT user_id FROM users WHERE login_id = ?")) {
                    statement.setString(1, "professor1");
                    try (var rows = statement.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                        faculty = rows.getLong(1);
                    }
                }
                for (String feature : MAPPERS.keySet()) {
                    Map<String, Object> query = query(feature, faculty);
                    List<Object> rows = session.selectList(namespace(feature) + ".list", query);
                    Long count = session.selectOne(namespace(feature) + ".count", query);
                    assertThat(rows).hasSize(count.intValue()).hasSizeGreaterThanOrEqualTo(3);
                    Object first = rows.get(0);
                    Long id = first instanceof Map<?, ?> map ? ((Number) map.get("achievementId")).longValue()
                            : (Long) first.getClass().getMethod("achievementId").invoke(first);
                    Map<String, Object> identity = Map.of("id", id, "achievementId", id, "lock", false);
                    Object detail = session.selectOne(namespace(feature) + ".find", identity);
                    assertThat(detail).isEqualTo(first);
                    try (var update = connection.prepareStatement("""
                            UPDATE education_achievements
                            SET deleted_yn = 'Y', achievement_status = 'DELETED'
                            WHERE achievement_id = ?
                            """)) {
                        update.setLong(1, id);
                        assertThat(update.executeUpdate()).isEqualTo(1);
                    }
                    session.clearCache();
                    assertThat((Object) session.selectOne(namespace(feature) + ".find", identity)).isNull();
                    assertThat((Long) session.selectOne(namespace(feature) + ".count", query)).isEqualTo(count - 1);
                    try (var statement = connection.prepareStatement(
                            "SELECT COUNT(*) FROM education_achievements WHERE achievement_id = ?")) {
                        statement.setLong(1, id);
                        try (var retained = statement.executeQuery()) {
                            assertThat(retained.next()).isTrue();
                            assertThat(retained.getLong(1)).isEqualTo(1);
                        }
                    }
                }
            } finally {
                connection.rollback();
            }
        }
    }
}
