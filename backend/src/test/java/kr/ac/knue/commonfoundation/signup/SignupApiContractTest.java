package kr.ac.knue.commonfoundation.signup;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knue.commonfoundation.common.api.CodedResponseException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SignupApiContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean SignupService signupService;

    @Test
    void checkSignupLoginIdReturnsDuplicateCodeWithoutSessionRequirement() throws Exception {
        when(signupService.checkLoginId("test_active")).thenThrow(new CodedResponseException(
                HttpStatus.CONFLICT, "DUPLICATE_USER_ID", "이미 사용 중인 아이디입니다.",
                List.of(new ValidationError("loginId", "이미 사용 중인 아이디입니다."))));

        mockMvc.perform(get("/api/auth/signup/check-login-id").param("loginId", "test_active"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_USER_ID"))
                .andExpect(jsonPath("$.error.fields[0].field").value("loginId"));
    }

    @Test
    void checkSignupEmailReturnsInvalidEmailFieldError() throws Exception {
        when(signupService.checkEmail("not-an-email")).thenThrow(new CodedResponseException(
                HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "올바른 이메일 형식이 아닙니다.",
                List.of(new ValidationError("email", "올바른 이메일 형식이 아닙니다."))));

        mockMvc.perform(get("/api/auth/signup/check-email").param("email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_EMAIL"))
                .andExpect(jsonPath("$.error.fields[0].field").value("email"));
    }

    @Test
    void createSignupReturnsCreatedEnvelopeAndDoesNotExposePasswordOrCookie() throws Exception {
        when(signupService.createSignup(org.mockito.ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(new SignupResponse("signupuser01", "signupuser01@example.test", "PENDING_EMAIL",
                        "인증 메일이 발송되었습니다. 이메일을 확인해주세요.", true));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"signupuser01","password":"Strong!123","passwordConfirm":"Strong!123","email":"signupuser01@example.test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountStatus").value("PENDING_EMAIL"))
                .andExpect(jsonPath("$.data.resendAvailable").value(true))
                .andExpect(content().string(not(containsString("Strong!123"))));
    }

    @Test
    void createSignupValidationUsesSpecifiedErrorCodes() throws Exception {
        when(signupService.createSignup(org.mockito.ArgumentMatchers.any(SignupRequest.class))).thenThrow(
                new CodedResponseException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH",
                        "비밀번호와 비밀번호 확인이 일치하지 않습니다.",
                        List.of(new ValidationError("passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다."))));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"signupuser01","password":"Strong!123","passwordConfirm":"Strong!124","email":"signupuser01@example.test"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_MISMATCH"))
                .andExpect(jsonPath("$.error.fields[0].field").value("passwordConfirm"));
    }
}
