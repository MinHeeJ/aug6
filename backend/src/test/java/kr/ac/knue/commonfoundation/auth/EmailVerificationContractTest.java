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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
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
class EmailVerificationContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    void validTokenActivationReturnsActiveVerifiedAccountWithoutSessionCookie() throws Exception {
        when(authService.updateEmailVerification(any(EmailVerificationRequest.class))).thenReturn(
                new EmailVerificationResponse(77L, "newuser", "user@example.com", "ACTIVE", "Y")
        );

        mockMvc.perform(post("/api/auth/email-verifications/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.emailVerifiedYn").value("Y"));
    }

    @Test
    void malformedTokenReturnsFieldErrorAndDoesNotUseToken() throws Exception {
        mockMvc.perform(post("/api/auth/email-verifications/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'token')]").exists());
        verify(authService, never()).updateEmailVerification(any(EmailVerificationRequest.class));
    }

    @Test
    void expiredUsedSupersededOrTamperedTokenReturnsSafeGuidance() throws Exception {
        when(authService.updateEmailVerification(any(EmailVerificationRequest.class))).thenThrow(
                new BusinessValidationException("만료된 인증 링크입니다. 인증 메일 재발송을 요청하세요.",
                        List.of(new ValidationError("token", "만료된 인증 링크입니다.")))
        );

        mockMvc.perform(post("/api/auth/email-verifications/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'token')]").exists());
    }

    @Test
    void suspendedAccountVerificationReturnsConflictWithoutSessionCookie() throws Exception {
        when(authService.updateEmailVerification(any(EmailVerificationRequest.class))).thenThrow(
                new ConflictException("현재 계정 상태에서는 이메일 인증으로 활성화할 수 없습니다.")
        );

        mockMvc.perform(post("/api/auth/email-verifications/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd\"}"))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }
}
