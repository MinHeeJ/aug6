package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;
import javax.sql.DataSource;
import kr.ac.knue.commonfoundation.courseoperations.CourseOperationMapper;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementMapper;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementMapper;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Bootstraps the application's alias package and all XML mappers without opening a database.
 * The selected-engine materialization check is opt-in and requires an already migrated throwaway DB.
 */
class EducationMapperIntegrationTest {
    private SqlSessionFactory factory(DataSource datasource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(datasource);
        bean.setTypeAliasesPackage("kr.ac.knue.commonfoundation");
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
        var configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        return bean.getObject();
    }

    @Test
    void mergedAliasRegistryAndEveryFeatureMapperBootstrapTogetherWithoutCollisions() throws Exception {
        var configuration = factory(mock(DataSource.class)).getConfiguration();
        for (Class<?> mapper : new Class<?>[]{CourseOperationMapper.class, LectureImprovementMapper.class,
                EmploymentRateImprovementMapper.class, EmploymentRateAchievementMapper.class}) {
            assertThat(configuration.hasMapper(mapper)).isTrue();
            for (var method : mapper.getDeclaredMethods()) {
                assertThat(configuration.hasStatement(mapper.getName() + "." + method.getName()))
                        .as("XML binding for %s.%s", mapper.getSimpleName(), method.getName()).isTrue();
            }
        }
    }

    @Test
    void postgresSeededActorRowsMaterializeThroughAllFourActualMappers() throws Exception {
        String url = System.getenv("TEST_POSTGRES_URL");
        assumeTrue(url != null && !url.isBlank(), "TEST_POSTGRES_URL absent: selected-engine readback not executed");
        String username = System.getenv("TEST_POSTGRES_USERNAME");
        String password = System.getenv("TEST_POSTGRES_PASSWORD");
        var source = new UnpooledDataSource("org.postgresql.Driver", url, username, password);
        try (Connection connection = DriverManager.getConnection(url, username, password);
                var session = factory(source).openSession(connection)) {
            long teacher = scalar(connection, "SELECT user_id FROM users WHERE login_id = ?", "professor1");
            long courseId = seed(connection, "COURSE_OPERATION", teacher);
            var course = session.getMapper(CourseOperationMapper.class).find(courseId, false);
            assertThat(course.teacherUserId()).isEqualTo(teacher);
            assertThat(course.attachmentIds()).isNotNull();
            assertThat(course.evaluationYear()).isEqualTo("2026");
            long lectureId = seed(connection, "LECTURE_IMPROVEMENT", teacher);
            var lecture = session.getMapper(LectureImprovementMapper.class).find(lectureId);
            assertThat(lecture.teacherUserId()).isEqualTo(teacher);
            assertThat(lecture.attachmentIds()).isNotNull();
            long improvementId = seed(connection, "EMPLOYMENT_RATE_IMPROVEMENT", teacher);
            var improvement = session.getMapper(EmploymentRateImprovementMapper.class).find(improvementId, false);
            assertThat(improvement.teacherUserId()).isEqualTo(teacher);
            long rateId = seed(connection, "EMPLOYMENT_RATE_ACHIEVEMENT", teacher);
            var rate = session.getMapper(EmploymentRateAchievementMapper.class).find(Map.of("achievementId", rateId));
            assertThat(rate.get("teacherUserId")).isEqualTo(teacher);
            assertThat(rate.get("achievementId")).isEqualTo(rateId);
            assertThat(rate.get("achievementStatus")).isEqualTo("DRAFT");
        }
    }

    private long seed(Connection connection, String type, long teacher) throws Exception {
        return scalar(connection, """
                SELECT a.achievement_id
                FROM education_achievements a
                WHERE a.achievement_type = ?
                  AND a.teacher_user_id = ?
                  AND a.achievement_status = 'DRAFT'
                  AND a.deleted_yn = 'N'
                ORDER BY a.achievement_id
                LIMIT 1
                """, type, teacher);
    }

    private long scalar(Connection connection, String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).as("existing seeded actor and scoped achievement required").isTrue();
                return rows.getLong(1);
            }
        }
    }
}
