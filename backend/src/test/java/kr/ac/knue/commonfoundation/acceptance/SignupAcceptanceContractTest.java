package kr.ac.knue.commonfoundation.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.SignupRequest;
import kr.ac.knue.commonfoundation.auth.SignupResponse;
import kr.ac.knue.commonfoundation.auth.SignupService;
import kr.ac.knue.commonfoundation.auth.UserIdAvailabilityResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Acceptance coverage for the anonymous signup HTTP contract and its durable fixtures.
 */
@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class SignupAcceptanceContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private SignupService signupService;

    @Test
    void signupOperationsAndRequiredSideEffectsAreRecordedInTheOpenApiFixture()
            throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        assertThat(openApi.exists()).isTrue();

        String contract = new String(openApi.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(contract)
                .contains("/api/v1/auth/check-userid:")
                .contains("operationId: checkUserIdAvailability")
                .contains("/api/v1/auth/signup:")
                .contains("operationId: signup")
                .contains(
                        "x-business-rules: [서버 검증 순서를 준수하고 password는 Argon2id hash만 저장한다]")
                .contains("x-side-effects: [users 행 생성, user_roles R01 행 생성]")
                .contains(
                        "passwordConfirm 불일치 -> 400 fields.passwordConfirm과 users 무생성이 반환된다")
                .contains(
                        "성공 가입 -> users.password_hash Argon2id와 user_roles R01 행이 존재한다");
    }

    @Test
    void anonymousRequestsCanCheckAvailabilityAndSignUpWithoutASessionCookie()
            throws Exception {
        when(signupService.checkUserIdAvailability("newuser1"))
                .thenReturn(new UserIdAvailabilityResponse(true));
        when(signupService.signup(any(SignupRequest.class)))
                .thenReturn(new SignupResponse("newuser1", "가입이 완료되었습니다."));

        mockMvc.perform(get("/api/v1/auth/check-userid").param("userId", "newuser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"newuser1","password":"Abcd!234",
                                "passwordConfirm":"Abcd!234","email":"newuser1@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("newuser1"))
                .andExpect(jsonPath("$.data.message").value("가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordConfirm").doesNotExist());
    }

    @Test
    void mismatchAndNormalizedDuplicateEmailUseTheOpenApiErrorStatuses()
            throws Exception {
        when(signupService.signup(any(SignupRequest.class)))
                .thenThrow(new BusinessValidationException(
                        "비밀번호와 비밀번호 확인이 일치하지 않습니다.",
                        java.util.List.of(new ValidationError(
                                "passwordConfirm",
                                "비밀번호와 비밀번호 확인이 일치하지 않습니다."))));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"newuser1","password":"Abcd!234",
                                "passwordConfirm":"Other!234","email":"newuser1@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(
                        jsonPath("$.error.fields[?(@.field == 'passwordConfirm')]").exists());

        when(signupService.signup(any(SignupRequest.class)))
                .thenThrow(new ConflictException("이미 등록된 이메일입니다."));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"newuser2","password":"Abcd!234",
                                "passwordConfirm":"Abcd!234","email":"TESTUSER1@EXAMPLE.COM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("이미 등록된 이메일입니다."));
    }

    @Test
    void authenticationFilterWhitelistsOnlyTheExactAnonymousSignupOperations()
            throws Exception {
        AuthService filterAuthService = mock(AuthService.class);
        EffectivePermissionService permissionService = mock(EffectivePermissionService.class);
        AuthenticationFilter filter = new AuthenticationFilter(
                filterAuthService,
                permissionService,
                new ObjectMapper());
        FilterChain anonymousChain = mock(FilterChain.class);

        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/auth/check-userid"),
                new MockHttpServletResponse(),
                anonymousChain);
        filter.doFilter(
                new MockHttpServletRequest("POST", "/api/v1/auth/signup"),
                new MockHttpServletResponse(),
                anonymousChain);

        MockHttpServletResponse protectedResponse = new MockHttpServletResponse();
        filter.doFilter(
                new MockHttpServletRequest("POST", "/api/v1/auth/other"),
                protectedResponse,
                mock(FilterChain.class));

        verify(anonymousChain, times(2)).doFilter(any(), any());
        verifyNoInteractions(filterAuthService, permissionService);
        assertThat(protectedResponse.getStatus()).isEqualTo(401);
    }

    @Test
    void migrationSeedsThreeLowercaseEmailArgon2idAccountsWithActiveManualR01Assignments()
            throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V65__basic85_signup.sql");
        assertThat(migration.exists()).isTrue();

        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ADD COLUMN IF NOT EXISTS email varchar(254)")
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email")
                .contains("'testuser1'")
                .contains("'testuser1@example.com'")
                .contains("'testuser2'")
                .contains("'testuser2@example.com'")
                .contains("'testuser3'")
                .contains("'testuser3@example.com'")
                .contains("$argon2id$v=19$")
                .contains("'R01'")
                .contains("'MANUAL'")
                .contains("'ACTIVE'");
    }
}
