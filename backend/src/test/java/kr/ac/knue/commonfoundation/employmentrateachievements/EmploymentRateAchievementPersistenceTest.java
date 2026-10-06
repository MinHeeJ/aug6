package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Opt-in PostgreSQL checks for real XML predicates, generated keys, snapshots and guard rollback. */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_EMPLOYMENT_RATE_DB_TESTS", matches = "true")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
class EmploymentRateAchievementPersistenceTest {
    @Autowired EmploymentRateAchievementMapper mapper;
    @Autowired EmploymentRateAchievementService service;
    @Autowired JdbcTemplate jdbc;
    private Long owner;
    private Long colleague;
    private String organization;
    private String marker;

    @BeforeEach
    void fixtures() {
        marker = "et-" + UUID.randomUUID();
        organization = jdbc.queryForObject("""
                SELECT a.organization_code
                FROM education_achievements a
                WHERE a.management_no = 'EDU-FR-032-2025-001'
                """, String.class);
        owner = createUser(marker + "-owner");
        colleague = createUser(marker + "-colleague");
        for (Long id : List.of(owner, colleague)) {
            jdbc.update("""
                    INSERT INTO organization_user_mappings (
                        organization_code, user_id, mapping_type, status
                    )
                    VALUES (?, ?, 'ORGANIZATION', 'ACTIVE')
                    """, organization, id);
            jdbc.update("""
                    INSERT INTO user_roles (user_id, role_code, status)
                    VALUES (?, 'R01', 'ACTIVE')
                    """, id);
        }
        jdbc.update("""
                INSERT INTO input_period_settings (
                    evaluation_year, area_code, organization_code, start_at, end_at, change_reason
                )
                VALUES ('2025', 'EDUCATION', ?, CURRENT_TIMESTAMP - INTERVAL '1 day',
                    CURRENT_TIMESTAMP + INTERVAL '1 day', 'test transaction only')
                """, organization);
    }

    @Test
    void generatedIdentityAndCreateUpdateSnapshotsSurviveRequery() {
        CurrentUser user = principal(owner, "R01");
        var create = new EmploymentRateAchievementRequest("FR-032", LocalDate.of(2025, 4, 10), marker, List.of());
        Map<String, Object> saved = service.create(create, user, marker);
        Map<?, ?> achievement = (Map<?, ?>) saved.get("achievement");
        Long id = ((Number) achievement.get("achievementId")).longValue();
        assertThat(service.detail(id, user)).containsEntry("achievementName", marker);
        var update = new EmploymentRateAchievementRequest("FR-032", LocalDate.of(2024, 12, 31),
                marker + "-changed", List.of());
        service.update(id, update, user, marker + "-update");
        assertThat(service.detail(id, user)).containsEntry("evaluationYear", "2025")
                .containsEntry("achievementName", marker + "-changed");
        String before = jdbc.queryForObject("""
                SELECT h.before_value
                FROM data_change_histories h
                WHERE h.request_id = ?
                """, String.class, marker + "-update");
        String after = jdbc.queryForObject("""
                SELECT h.after_value
                FROM data_change_histories h
                WHERE h.request_id = ?
                """, String.class, marker + "-update");
        assertThat(before).contains(marker, "2025-04-10");
        assertThat(after).contains(marker + "-changed", "2024-12-31", "2025");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM education_achievement_status_histories h
                WHERE h.achievement_type = 'FR-032' AND h.achievement_id = ? AND h.next_status = 'DRAFT'
                """, Integer.class, id)).isEqualTo(1);
    }

    @Test
    void listCountAndDetailUseUnionNotFirstRoleAndExactFilters() {
        long first = insert(owner, marker + "-own", LocalDate.of(2025, 4, 10));
        long second = insert(colleague, marker + "-colleague", LocalDate.of(2025, 4, 11));
        Map<String, Object> query = query();
        List<Map<String, Object>> own = mapper.list(query, owner, List.of("R01"));
        assertThat(own).hasSize(1);
        assertThat(mapper.count(query, owner, List.of("R01"))).isEqualTo(own.size());
        List<Map<String, Object>> union = mapper.list(query, owner, List.of("R01", "R02"));
        assertThat(union).hasSize(2);
        assertThat(mapper.count(query, owner, List.of("R01", "R02"))).isEqualTo(union.size());
        assertThat(mapper.visible(second, owner, List.of("R01"))).isZero();
        assertThat(mapper.visible(second, owner, List.of("R01", "R02"))).isEqualTo(1);
        query.put("managementNo", marker + "-own");
        assertThat(mapper.list(query, owner, List.of("R01", "R02"))).singleElement()
                .satisfies(row -> assertThat(((Number) row.get("achievementId")).longValue()).isEqualTo(first));
        assertThat(mapper.count(query, owner, List.of("R01", "R02"))).isEqualTo(1);
    }

    @Test
    void finalizedAndForeignUpdatesLeaveRowsAndHistoryUntouched() {
        long id = insert(owner, marker, LocalDate.of(2025, 4, 10));
        jdbc.update("""
                UPDATE education_achievements
                SET achievement_status = 'EVALUATION_CONFIRMED'
                WHERE achievement_id = ?
                """, id);
        var request = new EmploymentRateAchievementRequest("FR-032", LocalDate.of(2025, 4, 11), "changed", List.of());
        assertThatThrownBy(() -> service.update(id, request, principal(owner, "R01"), marker))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.update(id, request, principal(colleague, "R01"), marker))
                .isInstanceOf(ForbiddenException.class);
        assertThat(mapper.find(id, false)).containsEntry("achievementName", marker)
                .containsEntry("certificationStatus", "EVALUATION_CONFIRMED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM data_change_histories WHERE request_id = ?",
                Integer.class, marker)).isZero();
    }

    private Long createUser(String login) {
        return jdbc.queryForObject("""
                INSERT INTO users (login_id, password_hash, status, system_use_yn)
                VALUES (?, '!', 'ACTIVE', 'Y')
                RETURNING user_id
                """, Long.class, login);
    }

    private long insert(Long teacher, String name, LocalDate date) {
        Map<String, Object> values = new HashMap<>();
        values.put("managementNo", name);
        values.put("teacherUserId", teacher);
        values.put("organizationCode", organization);
        values.put("evaluationYear", "2025");
        values.put("managementItemCode", "FR-032");
        values.put("achievementDate", date);
        values.put("achievementName", name);
        values.put("attachmentIds", "[]");
        values.put("actor", teacher);
        mapper.insert(values);
        return ((Number) values.get("achievementId")).longValue();
    }

    private Map<String, Object> query() {
        Map<String, Object> query = new HashMap<>();
        query.put("page", 0);
        query.put("pageSize", 20);
        query.put("pageOffset", 0L);
        query.put("teacherName", marker);
        return query;
    }

    private CurrentUser principal(Long id, String... roles) {
        return new CurrentUser(id, marker, null, marker, List.of(roles), List.of());
    }
}
