package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Selected PostgreSQL persistence, real sessions and menu/function wiring; skipped without supplied connection. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
@Transactional(isolation = Isolation.SERIALIZABLE)
class EmploymentRateImprovementPersistenceIntegrationTest {
    private static final String BASE = "/api/business/employment-rate-improvements";
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    private Long teacherId;
    private Cookie cookie;

    @BeforeEach
    void preparePersistedPrincipalAndActivePeriods() {
        teacherId = jdbc.queryForObject("SELECT user_id FROM users WHERE login_id = 'professor1'", Long.class);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM user_roles
                WHERE user_id = ? AND role_code = 'R01' AND status = 'ACTIVE'
                """, Integer.class, teacherId)).isPositive();
        String session = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO sessions (session_id, user_id, expires_at)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '1 hour')
                """, session, teacherId);
        cookie = new Cookie(AuthController.SESSION_COOKIE, session);
        jdbc.update("""
                INSERT INTO input_period_settings (
                    evaluation_year, area_code, start_at, end_at, change_reason
                )
                VALUES ('2025', 'EDUCATION', CURRENT_TIMESTAMP - INTERVAL '1 day',
                    CURRENT_TIMESTAMP + INTERVAL '1 day', 'FR029 test period')
                """);
        jdbc.update("""
                INSERT INTO evaluation_date_settings (
                    evaluation_year, area_code, start_at, end_at, change_reason
                )
                VALUES ('2025', 'EDUCATION', TIMESTAMP '2025-01-01',
                    TIMESTAMP '2025-12-31', 'FR029 test evaluation period')
                """);
        jdbc.update("""
                DELETE FROM evaluation_finalizations
                WHERE target_user_id = ? AND evaluation_year = '2025'
                """, teacherId);
    }

    @Test
    void createUpdateAndConfirmedRefusalPreserveLedgerDetailAndCompleteHistory() throws Exception {
        String created = mvc.perform(post(BASE).cookie(cookie).header("X-Request-Id", "fr029-db-create")
                .contentType("application/json").content(body("처음 출제기간", "2025-04-10")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long id = json.readTree(created).path("data").path("achievement").path("achievementId").asLong();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM education_achievements a
                JOIN employment_rate_improvement_achievement_details d ON d.achievement_id = a.achievement_id
                WHERE a.achievement_id = ? AND a.teacher_user_id = ? AND a.achievement_status = 'DRAFT'
                """, Integer.class, id, teacherId)).isEqualTo(1);
        mvc.perform(put(BASE + "/" + id).cookie(cookie).header("X-Request-Id", "fr029-db-update")
                .contentType("application/json").content(body("수정 출제기간", "2026-01-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"));
        mvc.perform(get(BASE + "/" + id).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mockExamQuestionPeriod").value("수정 출제기간"))
                .andExpect(jsonPath("$.data.achievementDate").value("2026-01-01"));
        String storedDetail = jdbc.queryForObject("""
                SELECT a.achievement_detail ->> 'mockExamQuestionPeriod'
                FROM education_achievements a
                WHERE a.achievement_id = ?
                """, String.class, id);
        assertThat(storedDetail).isEqualTo("수정 출제기간");
        var history = jdbc.queryForMap("""
                SELECT before_value, after_value
                FROM data_change_histories
                WHERE target_business = 'education_achievements'
                  AND target_key = ? AND request_id = 'fr029-db-update'
                """, id.toString());
        JsonNode before = json.readTree((String) history.get("before_value"));
        JsonNode after = json.readTree((String) history.get("after_value"));
        assertThat(before.path("mockExamQuestionPeriod").asText()).isEqualTo("처음 출제기간");
        assertThat(after.path("mockExamQuestionPeriod").asText()).isEqualTo("수정 출제기간");
        assertThat(after.path("evaluationYear").asText()).isEqualTo(before.path("evaluationYear").asText());
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievement_status_histories
                WHERE achievement_id = ? AND achievement_type = 'FR-029' AND action_type = 'CREATE'
                """, Integer.class, id)).isEqualTo(1);
        jdbc.update("""
                UPDATE education_achievements
                SET achievement_status = 'EVALUATION_CONFIRMED'
                WHERE achievement_id = ?
                """, id);
        mvc.perform(put(BASE + "/" + id).cookie(cookie).contentType("application/json")
                .content(body("금지된 변경", "2025-04-10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        assertThat(jdbc.queryForObject("""
                SELECT mock_exam_question_period
                FROM employment_rate_improvement_achievement_details WHERE achievement_id = ?
                """, String.class, id)).isEqualTo("수정 출제기간");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM data_change_histories
                WHERE target_business = 'education_achievements' AND target_key = ?
                """, Integer.class, id.toString())).isEqualTo(2);
    }

    @Test
    void managementItemFromAnotherAchievementTypeIsRejectedWithoutWrites() throws Exception {
        long before = jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievements
                WHERE achievement_type = 'FR-029' AND teacher_user_id = ?
                """, Long.class, teacherId);
        mvc.perform(post(BASE).cookie(cookie).contentType("application/json")
                .content(body("다른 유형 관리항목", "2025-04-10")
                        .replace("\"FR-029\"", "\"FR-030\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievements
                WHERE achievement_type = 'FR-029' AND teacher_user_id = ?
                """, Long.class, teacherId)).isEqualTo(before);
    }

    @Test
    void multiRoleScopeUsesUnionForFilteredListTotalAndDetail() throws Exception {
        jdbc.update("""
                INSERT INTO user_roles (user_id, role_code, status)
                SELECT ?, 'R02', 'ACTIVE'
                WHERE NOT EXISTS (
                    SELECT 1 FROM user_roles r
                    WHERE r.user_id = ? AND r.role_code = 'R02' AND r.status = 'ACTIVE'
                )
                """, teacherId, teacherId);
        String result = mvc.perform(get(BASE).cookie(cookie).param("managementItemCode", "FR-029"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long expected = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM education_achievements a
                JOIN employment_rate_improvement_achievement_details d ON d.achievement_id = a.achievement_id
                WHERE a.achievement_type = 'FR-029' AND a.deleted_yn = 'N' AND a.management_item_code = 'FR-029'
                  AND (a.teacher_user_id = ? OR EXISTS (
                      SELECT 1 FROM organization_user_mappings m
                      WHERE m.user_id = ? AND m.organization_code = a.organization_code AND m.status = 'ACTIVE'
                        AND m.effective_start_date <= CURRENT_DATE
                        AND (m.effective_end_date IS NULL OR m.effective_end_date >= CURRENT_DATE)
                  ))
                """, Long.class, teacherId, teacherId);
        var data = json.readTree(result).path("data");
        assertThat(data.path("totalElements").asLong()).isEqualTo(expected);
        assertThat(data.path("achievements").size()).isEqualTo((int) Math.min(expected, 20));
        for (JsonNode row : data.path("achievements")) {
            mvc.perform(get(BASE + "/" + row.path("achievementId").asLong()).cookie(cookie))
                    .andExpect(status().isOk());
        }
    }

    private String body(String period, String date) throws Exception {
        return json.writeValueAsString(java.util.Map.of(
                "managementItemCode", "FR-029", "achievementDate", date,
                "specialLectureStartDate", "2025-04-01", "specialLectureEndDate", "2025-04-03",
                "mockExamQuestionPeriod", period));
    }
}
