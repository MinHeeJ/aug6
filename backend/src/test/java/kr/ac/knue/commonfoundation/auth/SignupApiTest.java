package kr.ac.knue.commonfoundation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class SignupApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;
    @MockBean SignupService signupService;

    @Test
    void openApiFixtureContainsSignupOperations() throws Exception {
        ClassPathResource openApi = new ClassPathResource("contracts/openapi.yaml");
        org.assertj.core.api.Assertions.assertThat(openApi.exists()).isTrue();
        String contract = new String(openApi.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(contract)
                .contains("operationId: checkUserIdAvailability")
                .contains("operationId: signup");
    }

    @Test
    void anonymousSignupReturns201WithoutSessionCookieOrPassword() throws Exception {
        when(signupService.signup(any(SignupRequest.class)))
                .thenReturn(new SignupResponse("newuser1", "가입이 완료되었습니다."));

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
    void signupReturnsValidationErrorBeforeServiceForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"BAD","password":"short","passwordConfirm":"different","email":"invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'userId')]").exists());
        verify(signupService, never()).signup(any(SignupRequest.class));
    }

    @Test
    void signupReturnsConflictForNormalizedDuplicateEmail() throws Exception {
        when(signupService.signup(any(SignupRequest.class)))
                .thenThrow(new ConflictException("이미 등록된 이메일입니다."));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"newuser1","password":"Abcd!234",
                                "passwordConfirm":"Abcd!234","email":"TESTUSER1@EXAMPLE.COM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("이미 등록된 이메일입니다."));
    }

    @Test
    void checkUserIdAvailabilityReturnsContractBoolean() throws Exception {
        when(signupService.checkUserIdAvailability("newuser1"))
                .thenReturn(new UserIdAvailabilityResponse(true));

        mockMvc.perform(get("/api/v1/auth/check-userid").param("userId", "newuser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }
}
