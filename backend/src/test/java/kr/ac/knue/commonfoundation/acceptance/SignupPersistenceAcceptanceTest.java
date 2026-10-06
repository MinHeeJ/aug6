package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthMapper;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.LocalAccountAuthenticationAdapter;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import kr.ac.knue.commonfoundation.permissions.PermissionMapper;
import kr.ac.knue.commonfoundation.signupimplementation.SignupController;
import kr.ac.knue.commonfoundation.signupimplementation.SignupMapper;
import kr.ac.knue.commonfoundation.signupimplementation.SignupPasswordEncoder;
import kr.ac.knue.commonfoundation.signupimplementation.SignupService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Real PostgreSQL/Flyway/MyBatis transaction readback and login compatibility in an isolated schema.
 * Requires SIGNUP_TEST_DATABASE_URL/USERNAME/PASSWORD and CREATE/DROP SCHEMA privileges.
 * Standalone MVC intentionally excludes servlet registration: it cannot prove anonymous filter reachability
 * or browser navigation. Runtime-created testuser fixtures are not evidence of a production seed migration.
 */
@EnabledIfEnvironmentVariable(named = "SIGNUP_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignupPersistenceAcceptanceTest {
    private final ObjectMapper json = new ObjectMapper();
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private MockMvc mvc;
    private SignupPasswordEncoder encoder;
    private String schema;
    private String password;

    @BeforeAll
    void initializeDisposableSchema() throws Exception {
        String url = System.getenv("SIGNUP_TEST_DATABASE_URL");
        String username = System.getenv("SIGNUP_TEST_DATABASE_USERNAME");
        String credential = System.getenv("SIGNUP_TEST_DATABASE_PASSWORD");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, username, credential));
        schema = "signup_acceptance_" + UUID.randomUUID().toString().replace("-", "");
        String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        DataSource source = new DriverManagerDataSource(scopedUrl, username, credential);
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> source);
        context.register(DatabaseConfiguration.class);
        context.refresh();
        encoder = context.getBean(SignupPasswordEncoder.class);
        AuthMapper authMapper = context.getBean(AuthMapper.class);
        EffectivePermissionService permissions = new EffectivePermissionService(
                context.getBean(PermissionMapper.class));
        AuthService auth = new AuthService(
                new LocalAccountAuthenticationAdapter(authMapper, permissions), authMapper, permissions);
        mvc = MockMvcBuilders.standaloneSetup(
                new SignupController(context.getBean(SignupService.class)), new AuthController(auth))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        password = "Aa1!" + UUID.randomUUID();
    }

    @AfterAll
    void dropOnlyGeneratedSchema() {
        try {
            if (context != null) {
                context.close();
            }
        } finally {
            if (admin != null && schema != null) {
                // Identifier derives exclusively from a local UUID, never caller-supplied input.
                admin.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    @Test
    void runtimeFixturesHaveDistinctLowercaseEmailsSaltedHashesAndOnlyR01() throws Exception {
        // Test-owned fixtures: never overwrite a legacy account or mistake these for deployed seed rows.
        for (int i = 1; i <= 3; i++) {
            String login = "testuser" + i;
            submit(input(login, login + "@example.invalid"))
                    .andExpect(status().isCreated());
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.user_id, u.login_id, u.email, u.password_hash, u.status, u.system_use_yn
                FROM users u
                WHERE u.login_id IN ('testuser1', 'testuser2', 'testuser3')
                ORDER BY u.login_id
                """);
        assertThat(rows).hasSize(3);
        assertThat(rows.stream().map(row -> row.get("email")).distinct().count()).isEqualTo(3L);
        assertThat(rows.stream().map(row -> row.get("password_hash")).distinct().count()).isEqualTo(3L);
        for (Map<String, Object> row : rows) {
            assertThat(row.get("status")).isEqualTo("ACTIVE");
            assertThat(row.get("system_use_yn")).isEqualTo("Y");
            assertThat(row.get("email")).isEqualTo(row.get("login_id") + "@example.invalid");
            String hash = (String) row.get("password_hash");
            assertThat(hash.startsWith("$argon2id$v=19$m=19456,t=2,p=1$")).isTrue();
            assertThat(encoder.matches(password, hash)).isTrue();
            List<Map<String, Object>> roles = jdbc.queryForList("""
                    SELECT ur.role_code, ur.assignment_type, ur.status,
                           ur.valid_start_date = CURRENT_DATE AS starts_today,
                           ur.valid_end_date, ur.approver_user_id
                    FROM user_roles ur
                    WHERE ur.user_id = ?
                    """, row.get("user_id"));
            assertThat(roles).hasSize(1);
            assertThat(roles.get(0)).containsEntry("role_code", "R01")
                    .containsEntry("assignment_type", "MANUAL").containsEntry("status", "ACTIVE")
                    .containsEntry("starts_today", true).containsEntry("valid_end_date", null)
                    .containsEntry("approver_user_id", null);
        }
    }

    @Test
    void createdAccountLogsInReadsSessionAndLogsOutWithoutAutomaticSignupSession() throws Exception {
        long sessions = countSessions();
        String login = uniqueLogin();
        String response = submit(input(login, "Mixed" + login + "@Example.Invalid"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(login))
                .andExpect(jsonPath("$.data.message").value("가입이 완료되었습니다."))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(response).path("data").size()).isEqualTo(2);
        assertThat(response).doesNotContain(password, "passwordConfirm", "passwordHash", "$argon2id$");
        assertThat(countSessions()).isEqualTo(sessions);
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE login_id = ?", String.class, login))
                .isEqualTo("mixed" + login + "@example.invalid");
        var session = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("loginId", login, "password", password))))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(jsonPath("$.data.roles[0]").value("R01"))
                .andReturn().getResponse().getCookie(AuthController.SESSION_COOKIE);
        assertThat(session).isNotNull();
        assertThat(countSessions()).isEqualTo(sessions + 1);
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.loginId").value(login));
        mvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isOk()).andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0));
        assertThat(jdbc.queryForObject("SELECT status FROM sessions WHERE session_id = ?",
                String.class, session.getValue())).isEqualTo("LOGGED_OUT");
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void everyRejectedValidationLeavesUsersRolesAndSessionsUnchanged() throws Exception {
        for (String field : List.of("userId", "password", "passwordConfirm", "email")) {
            Map<String, String> payload = input(uniqueLogin(), uniqueLogin() + "@example.invalid");
            payload.remove(field);
            rejectWithoutWrites(payload, 400, field);
        }
        for (String invalid : List.of("abc", "1abcd", "Uppercase", "abc_123", "abcdefghijklmnopqrstu")) {
            rejectWithoutWrites(input(invalid, uniqueLogin() + "@example.invalid"), 400, "userId");
        }
        for (String invalid : List.of("bad", "a".repeat(250) + "@example.invalid")) {
            rejectWithoutWrites(input(uniqueLogin(), invalid), 400, "email");
        }
        Map<String, String> mismatch = input(uniqueLogin(), uniqueLogin() + "@example.invalid");
        mismatch.put("passwordConfirm", password + "x");
        rejectWithoutWrites(mismatch, 400, "passwordConfirm");
        for (String invalid : List.of("Aa1!abc", "lower12345", "lowercase ", "lowercaseé")) {
            Map<String, String> payload = input(uniqueLogin(), uniqueLogin() + "@example.invalid");
            payload.put("password", invalid);
            payload.put("passwordConfirm", invalid);
            rejectWithoutWrites(payload, 400, "password");
        }
    }

    @Test
    void duplicateAndAvailabilityRespectAllStatusesAndOrderedEmailNormalization() throws Exception {
        String taken = uniqueLogin();
        String email = taken + "@example.invalid";
        submit(input(taken, email)).andExpect(status().isCreated());
        Map<String, Object> original = jdbc.queryForMap("SELECT * FROM users WHERE login_id = ?", taken);
        Map<String, String> duplicateId = input(taken, "bad");
        duplicateId.put("passwordConfirm", "different");
        rejectWithoutWrites(duplicateId, 409, "userId");
        Map<String, String> duplicateEmail = input(uniqueLogin(), email.toUpperCase(java.util.Locale.ROOT));
        duplicateEmail.put("passwordConfirm", "different");
        rejectWithoutWrites(duplicateEmail, 409, "email");
        assertThat(jdbc.queryForMap("SELECT * FROM users WHERE login_id = ?", taken)).isEqualTo(original);
        for (String state : List.of("ACTIVE", "INACTIVE", "DELETED")) {
            jdbc.update("UPDATE users SET status = ? WHERE login_id = ?", state, taken);
            mvc.perform(get("/api/v1/auth/check-userid").param("userId", taken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
            rejectWithoutWrites(input(taken, email), 409, "userId");
        }
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", uniqueLogin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/v1/auth/check-userid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("userId"));
    }

    private void rejectWithoutWrites(Map<String, String> input, int statusCode, String field) throws Exception {
        List<Map<String, Object>> users = jdbc.queryForList("SELECT * FROM users ORDER BY user_id");
        List<Map<String, Object>> roles = jdbc.queryForList("SELECT * FROM user_roles ORDER BY assignment_id");
        long sessions = countSessions();
        String response = submit(input).andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[0].field").value(field))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(password, "$argon2id$", "password_hash", "INSERT INTO");
        assertThat(jdbc.queryForList("SELECT * FROM users ORDER BY user_id")).isEqualTo(users);
        assertThat(jdbc.queryForList("SELECT * FROM user_roles ORDER BY assignment_id")).isEqualTo(roles);
        assertThat(countSessions()).isEqualTo(sessions);
    }

    private String uniqueLogin() {
        return "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private Map<String, String> input(String login, String email) {
        return new HashMap<>(Map.of(
                "userId", login, "password", password, "passwordConfirm", password, "email", email));
    }

    private ResultActions submit(Map<String, String> payload) throws Exception {
        return mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)));
    }

    private long countSessions() {
        return jdbc.queryForObject("SELECT count(*) FROM sessions", Long.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class DatabaseConfiguration {
        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setMapperLocations(new ClassPathResource("mapper/signupimplementation/SignupMapper.xml"));
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.addMapper(AuthMapper.class);
            configuration.addMapper(PermissionMapper.class);
            factory.setConfiguration(configuration);
            return factory.getObject();
        }

        @Bean
        SignupMapper signupMapper(SqlSessionFactory factory) {
            return new SqlSessionTemplate(factory).getMapper(SignupMapper.class);
        }

        @Bean
        AuthMapper authMapper(SqlSessionFactory factory) {
            return new SqlSessionTemplate(factory).getMapper(AuthMapper.class);
        }

        @Bean
        PermissionMapper permissionMapper(SqlSessionFactory factory) {
            return new SqlSessionTemplate(factory).getMapper(PermissionMapper.class);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }

        @Bean
        SignupPasswordEncoder encoder() {
            return new SignupPasswordEncoder();
        }

        @Bean
        SignupService signupService(SignupMapper mapper, SignupPasswordEncoder encoder) {
            return new SignupService(mapper, encoder);
        }
    }
}
