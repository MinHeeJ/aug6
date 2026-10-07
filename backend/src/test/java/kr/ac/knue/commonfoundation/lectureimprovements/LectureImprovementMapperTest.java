package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Exercises actual XML dynamic SQL and named parameter properties without requiring PostgreSQL. */
class LectureImprovementMapperTest {
    @Test
    void listAndCountShareRoleUnionAndOptionalPredicates() throws Exception {
        Configuration config = configuration();
        var criteria = new LectureImprovementSearchCriteria(1, 50, 50, "LECTURE_IMPROVEMENT", null, null, "DRAFT");
        var parameters = Map.of("criteria", criteria, "userId", 101L, "roles", List.of("R01", "R02", "R04"));
        String list = sql(config, "list", parameters);
        String count = sql(config, "count", parameters);
        assertThat(list.substring(list.indexOf("WHERE"), list.indexOf("ORDER BY")).trim())
                .isEqualTo(count.substring(count.indexOf("WHERE")));
        assertThat(list).contains("OR a.teacher_user_id = ?", "OR EXISTS", "pm.data_scope", "LIMIT ? OFFSET ?");
        assertThat(list).doesNotContain("? IS NULL", "COALESCE");
    }

    @Test
    void administratorScopeBypassDoesNotRemoveResourceOrDeletedFilters() throws Exception {
        var parameters = Map.of("criteria", new LectureImprovementSearchCriteria(0, 20, 0, null, null, null, null),
                "userId", 1L, "roles", List.of("R09"));
        String sql = sql(configuration(), "list", parameters);
        assertThat(sql).contains("a.achievement_type = 'LECTURE_IMPROVEMENT'", "a.deleted_yn = 'N'")
                .doesNotContain("a.teacher_user_id = ?", "ILIKE");
    }

    private Configuration configuration() throws Exception {
        Configuration config = new Configuration();
        String resource = "mapper/lectureimprovements/LectureImprovementMapper.xml";
        try (var input = new ClassPathResource(resource).getInputStream()) {
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }
        return config;
    }
    private String sql(Configuration config, String operation, Map<String, ?> params) {
        return config.getMappedStatement(LectureImprovementMapper.class.getName() + "." + operation)
                .getBoundSql(params).getSql().replaceAll("\\s+", " ").trim();
    }
}
