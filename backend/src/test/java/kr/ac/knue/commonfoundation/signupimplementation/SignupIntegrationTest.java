package kr.ac.knue.commonfoundation.signupimplementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Real PostgreSQL/Flyway + HTTP transaction checks in a disposable schema, not an in-memory substitute.
 * Requires SIGNUP_TEST_DATABASE_URL/USERNAME/PASSWORD. Servlet filter wiring is verified by the wiring slice.
 */
@EnabledIfEnvironmentVariable(named = "SIGNUP_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignupIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private DataSource administrativeSource;
    private MockMvc mvc;
    private SignupPasswordEncoder encoder;
    private String schema;
    private String password;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void database() throws Exception {
        String url = System.getenv("SIGNUP_TEST_DATABASE_URL");
        String username = System.getenv("SIGNUP_TEST_DATABASE_USERNAME");
        String credential = System.getenv("SIGNUP_TEST_DATABASE_PASSWORD");
        administrativeSource = new DriverManagerDataSource(url, username, credential);
        schema = "signup_test_" + UUID.randomUUID().toString().replace("-", "");
        String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        DataSource scopedSource = new DriverManagerDataSource(scopedUrl, username, credential);
        Flyway.configure().dataSource(scopedSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(scopedSource);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> scopedSource);
        context.register(DatabaseConfiguration.class);
        context.refresh();
        encoder = context.getBean(SignupPasswordEncoder.class);
        password = "Aa1!" + UUID.randomUUID();
        SignupService service = context.getBean(SignupService.class);
        AuthMapper authMapper = context.getBean(AuthMapper.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        when(permissions.visibleMenus(any(), any())).thenReturn(List.of());
        LocalAccountAuthenticationAdapter adapter = new LocalAccountAuthenticationAdapter(authMapper, permissions);
        AuthService authService = new AuthService(adapter, authMapper, permissions);
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(service), new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        // Approved acceptance fixtures are test-only, salted at execution, never shipped as production accounts.
        for (int i = 1; i <= 3; i++) {
            String login = "testuser" + i;
            submit(login, password, login + "@example.invalid").andExpect(status().isCreated());
        }
    }

    @AfterAll
    void cleanup() {
        if (context != null) {
            context.close();
        }
        if (administrativeSource != null && schema != null) {
            // Identifier is generated locally from a UUID, never from request or environment input.
            new JdbcTemplate(administrativeSource).execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void createPersistsGeneratedAccountR01HashAndAllowsExistingLoginFlow() throws Exception {
        long sessions = count("sessions");
        submit("persistteacher", password, "NewTeacher@Example.Invalid")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value("persistteacher"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT user_id, email, password_hash, status, system_use_yn FROM users WHERE login_id = ?",
                "persistteacher");
        Long id = ((Number) row.get("user_id")).longValue();
        assertThat(row.get("email")).isEqualTo("newteacher@example.invalid");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        assertThat(row.get("system_use_yn")).isEqualTo("Y");
        String hash = (String) row.get("password_hash");
        assertThat(hash).startsWith("$argon2id$").isNotEqualTo(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        Map<String, Object> role = jdbc.queryForMap(
                "SELECT role_code, assignment_type, status, valid_end_date, approver_user_id "
                        + "FROM user_roles WHERE user_id = ?", id);
        assertThat(role.get("role_code")).isEqualTo("R01");
        assertThat(role.get("assignment_type")).isEqualTo("MANUAL");
        assertThat(role.get("status")).isEqualTo("ACTIVE");
        assertThat(role.get("valid_end_date")).isNull();
        assertThat(role.get("approver_user_id")).isNull();
        assertThat(count("sessions")).isEqualTo(sessions);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("loginId", "persistteacher", "password", password))))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(jsonPath("$.data.roles[0]").value("R01"));
        assertThat(count("sessions")).isEqualTo(sessions + 1);
    }

    @Test
    void duplicateEmailAndMismatchLeaveBothTablesUnchanged() throws Exception {
        long users = count("users");
        long roles = count("user_roles");
        String originalHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE login_id = ?", String.class, "testuser2");
        submit("duplicatemail", password, "TESTUSER2@EXAMPLE.INVALID")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.fields[0].field").value("email"));
        mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", "mismatchteacher", "password", password,
                                "passwordConfirm", password + "x", "email", "mismatch@example.invalid"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("passwordConfirm"));
        assertThat(count("users")).isEqualTo(users);
        assertThat(count("user_roles")).isEqualTo(roles);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE login_id = ?",
                String.class, "testuser2")).isEqualTo(originalHash);
    }

    @Test
    void roleWriteFailureActuallyRollsBackAccountInsert() throws Exception {
        long users = count("users");
        long roles = count("user_roles");
        jdbc.execute("""
                CREATE FUNCTION reject_signup_role() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM users
                        WHERE user_id = NEW.user_id AND login_id = 'rollbackteacher'
                    ) THEN
                        RAISE EXCEPTION 'isolated role-write failure';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER reject_signup_role BEFORE INSERT ON user_roles
                FOR EACH ROW EXECUTE FUNCTION reject_signup_role()
                """);
        try {
            String response = submit("rollbackteacher", password, "rollback@example.invalid")
                    .andExpect(status().isInternalServerError())
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain(password, "isolated role-write", "INSERT", "password_hash");
            assertThat(count("users")).isEqualTo(users);
            assertThat(count("user_roles")).isEqualTo(roles);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE login_id = ?",
                    Long.class, "rollbackteacher")).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER reject_signup_role ON user_roles");
            jdbc.execute("DROP FUNCTION reject_signup_role()");
        }
    }

    @Test
    void occupancyIncludesDeletedAccountsAndFixturesContainOnlyR01() throws Exception {
        jdbc.update("UPDATE users SET status = 'DELETED' WHERE login_id = ?", "testuser3");
        try {
            mvc.perform(get("/api/v1/auth/check-userid").param("userId", "testuser3"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
            mvc.perform(get("/api/v1/auth/check-userid").param("userId", "unusedteacher"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
            for (int i = 1; i <= 3; i++) {
                String login = "testuser" + i;
                List<String> roleCodes = jdbc.queryForList("""
                        SELECT ur.role_code
                        FROM user_roles ur
                        JOIN users u ON u.user_id = ur.user_id
                        WHERE u.login_id = ?
                        ORDER BY ur.role_code
                        """, String.class, login);
                assertThat(roleCodes).containsExactly("R01");
                String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE login_id = ?",
                        String.class, login);
                assertThat(encoder.matches(password, hash)).isTrue();
            }
        } finally {
            jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE login_id = ?", "testuser3");
        }
    }

    @Test
    void concurrentSignupCreatesOneAccountAndOneRole() throws Exception {
        long users = count("users");
        long roles = count("user_roles");
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> request = () ->
                    submit("concurrentteacher", password, "concurrent@example.invalid")
                            .andReturn().getResponse().getStatus();
            var results = executor.invokeAll(List.of(request, request));
            assertThat(List.of(results.get(0).get(), results.get(1).get()))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(count("users")).isEqualTo(users + 1);
            assertThat(count("user_roles")).isEqualTo(roles + 1);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*)
                    FROM users u
                    JOIN user_roles ur ON ur.user_id = u.user_id
                    WHERE u.login_id = ? AND ur.role_code = 'R01'
                    """, Long.class, "concurrentteacher")).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void legacyAdminLoginKeepsCookieAndRoleContract() throws Exception {
        String legacyHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE login_id = ?", String.class, "admin");
        assertThat(legacyHash).startsWith("sha256:");
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE login_id = ?",
                String.class, "admin")).isNull();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("loginId", "admin", "password", "admin"))))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(jsonPath("$.data.roles[0]").value("R09"));
        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE login_id = ?",
                String.class, "admin")).isEqualTo(legacyHash);
    }

    private ResultActions submit(String loginId, String rawPassword, String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "userId", loginId, "password", rawPassword, "passwordConfirm", rawPassword, "email", email))));
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class DatabaseConfiguration {
        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setMapperLocations(new ClassPathResource("mapper/signupimplementation/SignupMapper.xml"));
            org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
            config.setMapUnderscoreToCamelCase(true);
            config.addMapper(AuthMapper.class);
            factory.setConfiguration(config);
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
