package kr.ac.knue.commonfoundation.signup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knue.commonfoundation.common.api.CodedResponseException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmailVerificationApiContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean SignupService signupService;

    @Test
    void verifyEmailWithValidTokenRedirectsToSuccessResultScreen() throws Exception {
        when(signupService.verifyEmail("valid-token-from-mail"))
                .thenReturn(new EmailVerificationResult("success", "이메일 인증이 완료되었습니다. 로그인해주세요."));

        mockMvc.perform(get("/api/auth/verify-email").param("token", "valid-token-from-mail"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/email-verification/result?status=success"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void verifyEmailWithExpiredTokenReturnsTokenExpiredCode() throws Exception {
        when(signupService.verifyEmail("expired-token-from-mail")).thenThrow(new CodedResponseException(
                HttpStatus.GONE, "TOKEN_EXPIRED", "인증 링크가 만료되었습니다. 인증 메일 재발송을 요청해주세요.",
                List.of(new ValidationError("token", "인증 링크가 만료되었습니다. 인증 메일 재발송을 요청해주세요."))));

        mockMvc.perform(get("/api/auth/verify-email").param("token", "expired-token-from-mail"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.error.fields[0].field").value("token"));
    }

    @Test
    void verifyEmailWithUsedTokenReturnsInvalidTokenCode() throws Exception {
        when(signupService.verifyEmail("used-token-from-mail")).thenThrow(new CodedResponseException(
                HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "유효하지 않은 인증 링크입니다.",
                List.of(new ValidationError("token", "유효하지 않은 인증 링크입니다."))));

        mockMvc.perform(get("/api/auth/verify-email").param("token", "used-token-from-mail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.error.fields[0].field").value("token"));
    }

    @Test
    void verifyEmailWithMissingOrUnknownTokenReturnsInvalidTokenCodeWithoutSensitiveLeakage() throws Exception {
        when(signupService.verifyEmail("not-a-real-token")).thenThrow(new CodedResponseException(
                HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "유효하지 않은 인증 링크입니다.",
                List.of(new ValidationError("token", "유효하지 않은 인증 링크입니다."))));
        when(signupService.verifyEmail(null)).thenThrow(new CodedResponseException(
                HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "유효하지 않은 인증 링크입니다.",
                List.of(new ValidationError("token", "유효하지 않은 인증 링크입니다."))));

        mockMvc.perform(get("/api/auth/verify-email").param("token", "not-a-real-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("유효하지 않은 인증 링크입니다."));
        mockMvc.perform(get("/api/auth/verify-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void resendVerificationForPendingEmailReturnsPrivacySafeSuccessEnvelope() throws Exception {
        when(signupService.resendVerificationEmail(any(ResendVerificationRequest.class)))
                .thenReturn(new ResendVerificationResponse("인증 메일 재발송 요청이 접수되었습니다."));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test_pending@example.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("인증 메일 재발송 요청이 접수되었습니다."));
    }

    @Test
    void resendVerificationForAlreadyActiveEmailReturnsAlreadyVerifiedWithoutNewMailDisclosure() throws Exception {
        when(signupService.resendVerificationEmail(any(ResendVerificationRequest.class)))
                .thenReturn(new ResendVerificationResponse("이미 인증이 완료된 계정입니다."));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test_active@example.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("이미 인증이 완료된 계정입니다."));
    }

    @Test
    void resendVerificationForUnknownEmailReturnsSamePendingSuccessMessage() throws Exception {
        when(signupService.resendVerificationEmail(any(ResendVerificationRequest.class)))
                .thenReturn(new ResendVerificationResponse("인증 메일 재발송 요청이 접수되었습니다."));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("인증 메일 재발송 요청이 접수되었습니다."));
    }

    @Test
    void resendVerificationWithinOneMinuteReturnsRateLimitCodeAndFieldSafeError() throws Exception {
        when(signupService.resendVerificationEmail(any(ResendVerificationRequest.class))).thenThrow(new CodedResponseException(
                HttpStatus.TOO_MANY_REQUESTS, "RESEND_RATE_LIMITED", "인증 메일은 1분 후 다시 요청할 수 있습니다.",
                List.of(new ValidationError("email", "인증 메일은 1분 후 다시 요청할 수 있습니다."))));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test_pending@example.edu\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESEND_RATE_LIMITED"))
                .andExpect(jsonPath("$.error.fields[0].field").value("email"));
    }
}
