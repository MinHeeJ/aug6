package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the merged application's actual filters, controllers and PostgreSQL adapters.
 * Opt in only against a disposable migrated database. JUnit conditions run before Spring context creation;
 * ordinary backend-only runners skip this class rather than attempting the localhost datasource default.
 * Fixture sessions and role changes roll back with each synchronous MockMvc test.
 */
@EnabledIfEnvironmentVariable(named = "EDUCATION_ACCEPTANCE_DB", matches = "true")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.+")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_PASSWORD", matches = ".+")
@SpringBootTest(classes = CommonFoundationApplication.class)
@AutoConfigureMockMvc
@Transactional
class EducationApplicationAcceptanceTest {
    private static final String EMPLOYMENT_API = "/api/business/employment-rate-achievements";
    private static final String SESSION_COOKIE = "COMMON_FOUNDATION_SESSION";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    @ParameterizedTest
    @CsvSource({
        "FR-029,/api/business/employment-rate-improvements",
        "FR-030,/api/business/course-operations",
        "FR-031,/api/business/lecture-improvements",
        "FR-032,/api/business/employment-rate-achievements"
    })
    void ownerCanReadSeededDetailAndSupportedPageSizesThroughRealFilters(String type, String api) throws Exception {
        Map<String, Object> row = seededDraft(type);
        Cookie session = sessionFor(((Number) row.get("teacher_user_id")).longValue(), "R01");
        String requestId = UUID.randomUUID().toString();

        mvc.perform(get(api + "/" + row.get("achievement_id"))
                        .cookie(session).header("X-Request-Id", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementId").value(((Number) row.get("achievement_id")).longValue()))
                .andExpect(jsonPath("$.meta.requestId").value(requestId));

        for (int pageSize : new int[] {20, 50, 100}) {
            mvc.perform(get(api).cookie(session).param("pageSize", Integer.toString(pageSize)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pageSize").value(pageSize));
        }
        mvc.perform(get(api).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(20));
        mvc.perform(get(api).cookie(session).param("pageSize", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @ParameterizedTest
    @CsvSource({
        "FR-029,/api/business/employment-rate-improvements",
        "FR-030,/api/business/course-operations",
        "FR-031,/api/business/lecture-improvements",
        "FR-032,/api/business/employment-rate-achievements"
    })
    void invalidCreateDoesNotChangeLedgerDetailsOrHistories(String type, String api) throws Exception {
        Map<String, Object> row = seededDraft(type);
        Cookie session = sessionFor(((Number) row.get("teacher_user_id")).longValue(), "R01");
        Map<String, Long> before = counts();

        MvcResult result = mvc.perform(post(api).cookie(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields").isNotEmpty())
                .andReturn();

        assertNoSensitiveLeak(result);
        assertThat(counts()).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({
        "FR-029,/api/business/employment-rate-improvements",
        "FR-030,/api/business/course-operations",
        "FR-031,/api/business/lecture-improvements",
        "FR-032,/api/business/employment-rate-achievements"
    })
    void nonexistentDetailReturns404NotAnEmptySuccess(String type, String api) throws Exception {
        Map<String, Object> row = seededDraft(type);
        Cookie session = sessionFor(((Number) row.get("teacher_user_id")).longValue(), "R01");
        Long missingId = jdbc.queryForObject(
                "SELECT COALESCE(MAX(achievement_id), 0) + 1 FROM education_achievements", Long.class);

        MvcResult result = mvc.perform(get(api + "/" + missingId).cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").isNotEmpty())
                .andReturn();
        assertNoSensitiveLeak(result);
    }

    @ParameterizedTest
    @CsvSource({
        "FR-029,/api/business/employment-rate-improvements",
        "FR-030,/api/business/course-operations",
        "FR-031,/api/business/lecture-improvements",
        "FR-032,/api/business/employment-rate-achievements"
    })
    void logicalDeletionRetainsDatabaseRowButHidesDetail(String type, String api) throws Exception {
        Map<String, Object> row = seededDraft(type);
        long id = ((Number) row.get("achievement_id")).longValue();
        Cookie session = sessionFor(((Number) row.get("teacher_user_id")).longValue(), "R01");
        // Storage fixture only: there is deliberately no invented public DELETE or status-change endpoint.
        jdbc.update("""
                UPDATE education_achievements
                SET deleted_yn = 'Y', achievement_status = 'DELETED'
                WHERE achievement_id = ?
                """, id);

        mvc.perform(get(api + "/" + id).cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject(
                "SELECT deleted_yn FROM education_achievements WHERE achievement_id = ?", String.class, id))
                .isEqualTo("Y");
    }

    @ParameterizedTest
    @CsvSource({
        "FR-029,/api/business/employment-rate-improvements",
        "FR-030,/api/business/course-operations",
        "FR-031,/api/business/lecture-improvements",
        "FR-032,/api/business/employment-rate-achievements"
    })
    void teacherCannotReadAnotherOwnersDetail(String type, String api) throws Exception {
        Map<String, Object> own = seededDraft(type);
        long owner = ((Number) own.get("teacher_user_id")).longValue();
        Cookie session = sessionFor(owner, "R01");
        Long otherId = jdbc.queryForObject("""
                SELECT a.achievement_id
                FROM education_achievements a
                WHERE a.achievement_type = ?
                  AND a.teacher_user_id <> ?
                  AND a.deleted_yn = 'N'
                ORDER BY a.achievement_id
                LIMIT 1
                """, Long.class, type, owner);

        mvc.perform(get(api + "/" + otherId).cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void approvedSchemaSeedsHaveMetadataSnapshotsAndThreeExamplesPerNewTable() {
        for (Map.Entry<String, Long> count : counts().entrySet()) {
            assertThat(count.getValue()).as("migrated fixture rows in %s", count.getKey())
                    .isGreaterThanOrEqualTo(3L);
        }
        Integer incomplete = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM education_achievements a
                WHERE a.created_by IS NULL
                   OR a.updated_by IS NULL
                   OR a.created_at IS NULL
                   OR a.updated_at IS NULL
                """, Integer.class);
        assertThat(incomplete).isZero();
        Integer missingHistory = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM education_achievements a
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM data_change_histories h
                    WHERE h.target_business = a.achievement_type
                      AND h.target_key = a.achievement_id::text
                      AND h.field_name = 'snapshot'
                      AND h.after_value IS NOT NULL
                )
                """, Integer.class);
        assertThat(missingHistory).isZero();
    }

    @Test
    void administratorMenuBypassDoesNotAuthorizeR07BulkResultOperation() throws Exception {
        long userId = fixtureUser("R09");
        Cookie session = sessionFor(userId, "R09");
        String jobId = jdbc.queryForObject(
                "SELECT batch_job_id FROM employment_rate_batch_jobs ORDER BY batch_job_id LIMIT 1", String.class);
        Map<String, Long> before = counts();

        MvcResult result = mvc.perform(get(EMPLOYMENT_API + "/bulk-jobs/" + jobId).cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andReturn();

        assertNoSensitiveLeak(result);
        assertThat(counts()).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({
        "FR-029,/api/business/employment-rate-improvements",
        "FR-030,/api/business/course-operations",
        "FR-031,/api/business/lecture-improvements",
        "FR-032,/api/business/employment-rate-achievements"
    })
    void warmedAuthenticatedListHasMeanServerLatencyBelowThreeSeconds(String type, String api) throws Exception {
        Map<String, Object> row = seededDraft(type);
        Cookie session = sessionFor(((Number) row.get("teacher_user_id")).longValue(), "R01");
        mvc.perform(get(api).cookie(session)).andExpect(status().isOk());
        long elapsed = 0;
        int samples = 5;
        for (int index = 0; index < samples; index++) {
            long started = System.nanoTime();
            mvc.perform(get(api).cookie(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            elapsed += System.nanoTime() - started;
        }
        // Server-side smoke only: excludes browser rendering and network latency.
        assertThat(elapsed / samples).as("mean authenticated MockMvc list latency in nanoseconds")
                .isLessThan(3_000_000_000L);
    }

    @Test
    void migratedApplicationPreservesPublicHealthContract() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    private Map<String, Object> seededDraft(String type) {
        return jdbc.queryForMap("""
                SELECT a.achievement_id, a.teacher_user_id
                FROM education_achievements a
                WHERE a.achievement_type = ?
                  AND a.achievement_status = 'DRAFT'
                  AND a.deleted_yn = 'N'
                ORDER BY a.achievement_id
                LIMIT 1
                """, type);
    }

    private Cookie sessionFor(long userId, String role) {
        // Isolate the role matrix, not an administrator or accidentally multi-role seeded principal.
        jdbc.update("UPDATE user_roles SET status = 'INACTIVE' WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO user_roles (user_id, role_code, status) VALUES (?, ?, 'ACTIVE')", userId, role);
        jdbc.update("UPDATE users SET system_use_yn = 'Y', status = 'ACTIVE' WHERE user_id = ?", userId);
        String token = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO sessions (session_id, user_id, expires_at, status, login_at)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '1 hour', 'ACTIVE', CURRENT_TIMESTAMP)
                """, token, userId);
        return new Cookie(SESSION_COOKIE, token);
    }

    private long fixtureUser(String role) {
        Long userId = jdbc.queryForObject("""
                INSERT INTO users (login_id, password_hash, system_use_yn, status)
                VALUES (?, '!', 'Y', 'ACTIVE')
                RETURNING user_id
                """, Long.class, "acceptance-" + role + "-" + UUID.randomUUID());
        assertThat(userId).isNotNull();
        return userId;
    }

    private Map<String, Long> counts() {
        return Map.of(
                "ledger", count("education_achievements"),
                "employmentDetails", count("employment_rate_improvement_achievement_details"),
                "courseDetails", count("course_operation_achievement_details"),
                "lectureDetails", count("lecture_improvement_achievement_details"),
                "history", count("data_change_histories"),
                "statusHistory", count("education_achievement_status_histories"),
                "jobs", count("employment_rate_batch_jobs"),
                "items", count("employment_rate_batch_job_items"));
    }

    private long count(String table) {
        // Only the fixed test-owned identifiers above are passed; never user-controlled SQL identifiers.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private void assertNoSensitiveLeak(MvcResult result) throws Exception {
        JsonNode error = json.readTree(result.getResponse().getContentAsString()).path("error");
        assertThat(error.toString()).doesNotContain(
                "jdbc:", "SQLException", "org.postgresql", "stackTrace", "password_hash", "passwordHash");
    }
}
