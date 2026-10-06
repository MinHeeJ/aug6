package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthMapper;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.LocalAccountAuthenticationAdapter;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import kr.ac.knue.commonfoundation.signupimplementation.SignupController;
import kr.ac.knue.commonfoundation.signupimplementation.SignupMapper;
import kr.ac.knue.commonfoundation.signupimplementation.SignupPasswordEncoder;
import kr.ac.knue.commonfoundation.signupimplementation.SignupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP regression through the real credential adapter; mapper doubles do not prove database persistence. */
class SignupAuthenticationRegressionTest {
    private final ObjectMapper json = new ObjectMapper();
    private AuthMapper mapper;
    private EffectivePermissionService permissions;
    private AuthService service;
    private SignupPasswordEncoder encoder;
    private String password;

    @BeforeEach
    void setUp() {
        mapper = mock(AuthMapper.class);
        permissions = mock(EffectivePermissionService.class);
        encoder = new SignupPasswordEncoder();
        password = "Aa1!" + UUID.randomUUID();
        service = new AuthService(new LocalAccountAuthenticationAdapter(mapper, permissions), mapper, permissions);
        when(permissions.visibleMenus(any(), any())).thenReturn(List.of());
    }

    @Test
    void approvedContractFixtureIsAvailableInsideBackendBuildContext() throws Exception {
        // This is a resource prerequisite, not a substitute for the behavioral HTTP checks below.
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            String contract = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(contract).contains(
                    "/api/v1/auth/signup:", "operationId: signup",
                    "/api/v1/auth/check-userid:", "operationId: checkUserIdAvailability",
                    "SignupRequest:", "UserIdAvailabilityResponse:", "SignupResponse:");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"argon2id", "sha256"})
    void bothCredentialFormatsKeepLoginMeLogoutCookieContract(String format) throws Exception {
        String hash = format.equals("argon2id") ? encoder.encode(password) : "sha256:" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8)));
        List<String> roles = format.equals("argon2id") ? List.of("R01") : List.of("R09");
        when(mapper.findAccountByLoginId("regressionteacher")).thenReturn(
                new AuthMapper.AccountRow(71L, "regressionteacher", hash, null, "regressionteacher"));
        when(mapper.findActiveRoleCodes(71L)).thenReturn(roles);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("loginId", "regressionteacher", "password", password))))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(cookie().path(AuthController.SESSION_COOKIE, "/"))
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 28800))
                .andExpect(jsonPath("$.data.roles[0]").value(roles.get(0)))
                .andReturn().getResponse();
        assertThat(login.getContentAsString()).doesNotContain(password, hash, "passwordHash");
        var session = login.getCookie(AuthController.SESSION_COOKIE);
        assertThat(session).isNotNull();
        assertThat(login.getHeader("Set-Cookie")).contains("SameSite=Lax");
        verify(mapper).insertSession(eq(session.getValue()), eq(71L), any());
        when(mapper.findUserByActiveSession(session.getValue())).thenReturn(
                new AuthMapper.SessionUserRow(session.getValue(), 71L, "regressionteacher", null, "regressionteacher"));
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("regressionteacher"))
                .andExpect(jsonPath("$.data.roles[0]").value(roles.get(0)));
        verify(mapper).touchSession(session.getValue());
        mvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0));
        verify(mapper).logout(session.getValue());
        when(mapper.findUserByActiveSession(session.getValue())).thenReturn(null);
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongArgon2CredentialDoesNotCreateSessionOrLeakSecrets() throws Exception {
        when(mapper.findAccountByLoginId("regressionteacher")).thenReturn(
                new AuthMapper.AccountRow(71L, "regressionteacher", encoder.encode(password), null, "teacher"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "loginId", "regressionteacher", "password", password + "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(password, "$argon2id$", "password_hash");
        verify(mapper, never()).insertSession(any(), any(), any());
    }

    @Test
    void anonymousSignupOperationsReachControllersWithoutSessionLookup() throws Exception {
        SignupMapper signupMapper = mock(SignupMapper.class);
        when(signupMapper.insertAccount(any(), any(), any())).thenReturn(81L);
        when(signupMapper.insertDefaultRole(81L)).thenReturn(1);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new SignupController(new SignupService(signupMapper, encoder)))
                .addFilters(new AuthenticationFilter(service, permissions, json))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/auth/check-userid").param("userId", "newteacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", "newteacher",
                                "password", password,
                                "passwordConfirm", password,
                                "email", "newteacher@example.invalid"))))
                .andExpect(status().isCreated())
                .andExpect(cookie().doesNotExist(AuthController.SESSION_COOKIE));
        verify(signupMapper).insertDefaultRole(81L);
        verify(mapper, never()).findUserByActiveSession(any());
        verify(mapper, never()).insertSession(any(), any(), any());
    }

    @Test
    void anonymousSignupExemptionsDoNotIncludeWrongMethods() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .addFilters(new AuthenticationFilter(service, permissions, json))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/auth/signup")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/check-userid")).andExpect(status().isUnauthorized());
        verify(mapper, never()).findUserByActiveSession(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/users", "/api/auth/me", "/api/v1/auth/signup/extra",
            "/api/v1/auth/check-userid/extra", "/api/auth/reset-password", "/api/auth/verify-email"
    })
    void anonymousProtectionIsNotBroadenedToAdjacentOrExcludedRoutes(String path) throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .addFilters(new AuthenticationFilter(service, permissions, json))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(mapper, never()).findUserByActiveSession(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "abcdefghijklmnopqrst"})
    void userIdBoundaryAndThreeCategoriesWithoutPunctuationAreAccepted(String loginId) throws Exception {
        SignupMapper signupMapper = mock(SignupMapper.class);
        when(signupMapper.insertAccount(any(), any(), any())).thenReturn(81L);
        when(signupMapper.insertDefaultRole(81L)).thenReturn(1);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new SignupController(new SignupService(signupMapper, encoder))).build();
        // A/B, a/b, 1/2 are three categories. Punctuation is not individually mandatory (D2).
        String raw = "Aa12" + UUID.randomUUID().toString().replace("-", "");
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", loginId, "password", raw, "passwordConfirm", raw,
                                "email", loginId + "@example.invalid"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(loginId));
        verify(signupMapper).insertDefaultRole(81L);
    }
}
