package kr.ac.knue.commonfoundation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class SignupContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void signupValidationRejectsDuplicateLoginInvalidEmailWeakPasswordAndSubstituteAddress() throws Exception {
        when(authService.signup(any(SignupRequest.class))).thenThrow(new BusinessValidationException(
                "회원가입 입력값을 확인해 주세요.",
                List.of(
                        new ValidationError("loginId", "이미 사용 중인 로그인 ID입니다."),
                        new ValidationError("email", "올바른 이메일 주소를 입력하세요."),
                        new ValidationError("password", "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.")
                )
        ));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\" existing \",\"password\":\"short\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'loginId')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'password')]").exists());

        when(authService.signup(any(SignupRequest.class))).thenThrow(new BusinessValidationException(
                "회원가입 입력값을 확인해 주세요.",
                List.of(new ValidationError("email", "admin@kndadmin.com은 신규 회원가입에 사용할 수 없습니다."))
        ));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"newuser\",\"password\":\"Password1\",\"email\":\"admin@kndadmin.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'email')]").exists());
    }

    @Test
    void signupRejectsNormalizedDuplicateEmailWithConflictEnvelope() throws Exception {
        when(authService.signup(any(SignupRequest.class))).thenThrow(new ConflictException("이미 사용 중인 이메일입니다."));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"newuser\",\"password\":\"Password1\",\"email\":\" USER@Example.COM \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void signupSuccessReturnsPendingVerificationWithoutSessionCookie() throws Exception {
        when(authService.signup(any(SignupRequest.class))).thenReturn(new SignupResponse(
                42L,
                "newuser",
                "user@example.com",
                "PENDING_EMAIL",
                "N",
                "FAILED_OTHER"
        ));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\" newuser \",\"password\":\"Password1\",\"email\":\" USER@Example.COM \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.accountStatus").value("PENDING_EMAIL"))
                .andExpect(jsonPath("$.data.emailVerifiedYn").value("N"));
    }

    @Test
    void beanValidationRejectsBlankRequiredFieldsBeforeServiceSideEffects() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'loginId')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'email')]").exists());
        verify(authService, never()).signup(any(SignupRequest.class));
    }
}
