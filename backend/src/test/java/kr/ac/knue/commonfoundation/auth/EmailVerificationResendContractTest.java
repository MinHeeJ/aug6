package kr.ac.knue.commonfoundation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.TooManyRequestsException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class EmailVerificationResendContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void resendForPendingUserReturnsNeutralAcceptedEnvelopeWithoutSessionCookie() throws Exception {
        when(authService.createEmailVerificationResend(any(EmailVerificationResendRequest.class), any(String.class)))
                .thenReturn(new EmailVerificationResendResponse("ACCEPTED", "인증 메일 재발송 요청을 접수했습니다."));

        mockMvc.perform(post("/api/auth/email-verifications/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("{\"email\":\" USER@Example.COM \"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void malformedEmailReturnsFieldErrorBeforeServiceSideEffects() throws Exception {
        mockMvc.perform(post("/api/auth/email-verifications/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'email')]").exists());
        verify(authService, never()).createEmailVerificationResend(any(), any());
    }

    @Test
    void sameAccountOrEmailThrottleReturns429WithoutTokenReplacement() throws Exception {
        when(authService.createEmailVerificationResend(any(EmailVerificationResendRequest.class), any(String.class)))
                .thenThrow(new TooManyRequestsException("인증 메일은 60초 후 다시 요청할 수 있습니다."));

        mockMvc.perform(post("/api/auth/email-verifications/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void serviceValidationErrorUsesSafeEnvelopeWithoutEchoingSecretsOrTokens() throws Exception {
        when(authService.createEmailVerificationResend(any(EmailVerificationResendRequest.class), any(String.class)))
                .thenThrow(new BusinessValidationException("재발송 입력값을 확인해 주세요.",
                        List.of(new ValidationError("email", "올바른 이메일 주소를 입력하세요."))));

        mockMvc.perform(post("/api/auth/email-verifications/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"token\":\"0123456789abcdef\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("재발송 입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'email')]").exists());
    }
}
