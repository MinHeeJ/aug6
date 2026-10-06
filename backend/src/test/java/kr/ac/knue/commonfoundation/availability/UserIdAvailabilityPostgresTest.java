package kr.ac.knue.commonfoundation.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Uses the selected PostgreSQL engine and real mapper; fixture writes roll back after each test. */
@SpringBootTest(classes = CommonFoundationApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class UserIdAvailabilityPostgresTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE", "DELETED"})
    void allExistingIdentifiersRemainReservedWithoutSessionOrAccountChanges(String accountStatus) throws Exception {
        String id = identity();
        Long key = jdbc.queryForObject("""
                INSERT INTO users (login_id, password_hash, status, system_use_yn)
                VALUES (?, ?, ?, 'N')
                RETURNING user_id
                """, Long.class, id, UUID.randomUUID().toString(), accountStatus);
        Map<String, Object> before = jdbc.queryForMap("SELECT * FROM users WHERE user_id = ?", key);
        check(id, false);
        check(id, false);
        assertThat(jdbc.queryForMap("SELECT * FROM users WHERE user_id = ?", key)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id = ?", Long.class, key))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sessions WHERE user_id = ?", Long.class, key))
                .isZero();
    }

    @Test
    void unusedIdentifierIsAvailableAndIsNotInsertedByLookup() throws Exception {
        String id = identity();
        assertAbsent(id);
        check(id, true);
        check(id, true);
        assertAbsent(id);
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "1abc", "Abcd", "ab-c", "abcdefghijklmnopqrstu", " abcd "})
    void invalidIdentifierProduces400WithoutCreatingAnAccount(String id) throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("userId"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertAbsent(id);
    }

    private void check(String id, boolean expected) throws Exception {
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(expected))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private void assertAbsent(String id) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE login_id = ?", Long.class, id)).isZero();
    }

    private String identity() {
        return "avail" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
