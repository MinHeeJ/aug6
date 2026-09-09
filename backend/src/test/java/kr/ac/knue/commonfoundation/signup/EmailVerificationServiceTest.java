package kr.ac.knue.commonfoundation.signup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.CodedResponseException;
import kr.ac.knue.commonfoundation.mail.EmailVerificationMailDispatcher;
import kr.ac.knue.commonfoundation.mail.VerificationMailCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailVerificationServiceTest {
    SignupBoundaryPolicy policy = new SignupBoundaryPolicy();
    SignupMapper mapper = org.mockito.Mockito.mock(SignupMapper.class);
    PasswordHashService passwordHashService = org.mockito.Mockito.mock(PasswordHashService.class);
    SignupTokenService tokenService = org.mockito.Mockito.mock(SignupTokenService.class);
    EmailVerificationMailDispatcher dispatcher = org.mockito.Mockito.mock(EmailVerificationMailDispatcher.class);
    SignupService service = new SignupService(policy, mapper, passwordHashService, tokenService, dispatcher);

    @Test
    void validTokenUsesSha256LookupActivatesUserAndMarksTokenUsed() {
        String rawToken = "valid-token-from-mail";
        String tokenHash = "a".repeat(64);
        when(tokenService.sha256Hex(rawToken)).thenReturn(tokenHash);
        when(mapper.findEmailVerificationToken(tokenHash)).thenReturn(new SignupMapper.EmailVerificationTokenRow(
                10L, 77L, tokenHash, LocalDateTime.now().plusHours(1), "N", null, "PENDING_EMAIL", "N"));
        when(mapper.activateVerifiedUser(77L)).thenReturn(1);
        when(mapper.markEmailVerificationTokenUsed(10L)).thenReturn(1);

        EmailVerificationResult result = service.verifyEmail(rawToken);

        assertThat(result.status()).isEqualTo("success");
        verify(mapper).activateVerifiedUser(77L);
        verify(mapper).markEmailVerificationTokenUsed(10L);
    }

    @Test
    void expiredTokenIsRejectedWithoutAccountTransition() {
        String rawToken = "expired-token-from-mail";
        String tokenHash = "b".repeat(64);
        when(tokenService.sha256Hex(rawToken)).thenReturn(tokenHash);
        when(mapper.findEmailVerificationToken(tokenHash)).thenReturn(new SignupMapper.EmailVerificationTokenRow(
                11L, 78L, tokenHash, LocalDateTime.now().minusMinutes(1), "N", null, "PENDING_EMAIL", "N"));

        assertThatThrownBy(() -> service.verifyEmail(rawToken))
                .isInstanceOf(CodedResponseException.class)
                .hasMessageContaining("인증 링크가 만료되었습니다");
        verify(mapper, never()).activateVerifiedUser(78L);
        verify(mapper, never()).markEmailVerificationTokenUsed(11L);
    }

    @Test
    void usedTokenIsRejectedWithoutAccountTransition() {
        String rawToken = "used-token-from-mail";
        String tokenHash = "c".repeat(64);
        when(tokenService.sha256Hex(rawToken)).thenReturn(tokenHash);
        when(mapper.findEmailVerificationToken(tokenHash)).thenReturn(new SignupMapper.EmailVerificationTokenRow(
                12L, 79L, tokenHash, LocalDateTime.now().plusHours(1), "Y", LocalDateTime.now(), "PENDING_EMAIL", "N"));

        assertThatThrownBy(() -> service.verifyEmail(rawToken))
                .isInstanceOf(CodedResponseException.class)
                .hasMessageContaining("유효하지 않은 인증 링크입니다");
        verify(mapper, never()).activateVerifiedUser(79L);
        verify(mapper, never()).markEmailVerificationTokenUsed(12L);
    }

    @Test
    void unknownTokenIsRejectedAfterHashLookupOnly() {
        String rawToken = "invalid-token-from-mail";
        String tokenHash = "d".repeat(64);
        when(tokenService.sha256Hex(rawToken)).thenReturn(tokenHash);
        when(mapper.findEmailVerificationToken(tokenHash)).thenReturn(null);

        assertThatThrownBy(() -> service.verifyEmail(rawToken))
                .isInstanceOf(CodedResponseException.class)
                .hasMessageContaining("유효하지 않은 인증 링크입니다");
        verify(mapper).findEmailVerificationToken(tokenHash);
        verify(mapper, never()).activateVerifiedUser(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void resendForPendingEmailInvalidatesExistingTokensCreatesNewTokenAndDispatchesReusableHtmlMailTemplate() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail(" Test_Pending@Example.Edu ");
        when(mapper.findUserByEmail("test_pending@example.edu"))
                .thenReturn(new SignupMapper.SignupUserRow(77L, "test_pending", "test_pending@example.edu", "PENDING_EMAIL", "N"));
        when(mapper.countRecentResendAttempts("test_pending@example.edu")).thenReturn(0);
        when(tokenService.generateRawToken()).thenReturn("new-url-safe-resend-token-with-more-than-32-bytes");
        when(tokenService.sha256Hex("new-url-safe-resend-token-with-more-than-32-bytes")).thenReturn("e".repeat(64));

        ResendVerificationResponse response = service.resendVerificationEmail(request);

        assertThat(response.message()).isEqualTo("인증 메일 재발송 요청이 접수되었습니다.");
        verify(mapper).invalidatePendingEmailVerificationTokens(77L);
        verify(mapper).insertEmailVerificationToken(77L, "e".repeat(64));
        ArgumentCaptor<VerificationMailCommand> mail = ArgumentCaptor.forClass(VerificationMailCommand.class);
        verify(dispatcher).sendVerificationMail(mail.capture());
        assertThat(mail.getValue().loginId()).isEqualTo("test_pending");
        assertThat(mail.getValue().email()).isEqualTo("test_pending@example.edu");
        assertThat(mail.getValue().verificationToken()).isEqualTo("new-url-safe-resend-token-with-more-than-32-bytes");
    }

    @Test
    void resendForActiveEmailReturnsAlreadyVerifiedAndDoesNotCreateTokenOrSendMail() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test_active@example.edu");
        when(mapper.findUserByEmail("test_active@example.edu"))
                .thenReturn(new SignupMapper.SignupUserRow(78L, "test_active", "test_active@example.edu", "ACTIVE", "Y"));

        ResendVerificationResponse response = service.resendVerificationEmail(request);

        assertThat(response.message()).isEqualTo("이미 인증이 완료된 계정입니다.");
        verify(mapper).insertMailAttemptStatus(78L, "test_active@example.edu", "SKIPPED_ACTIVE", "RESEND:signup-request");
        verify(mapper, never()).insertEmailVerificationToken(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(dispatcher, never()).sendVerificationMail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resendForUnknownEmailReturnsSameSuccessMessageWithoutCreatingToken() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("unknown@example.edu");
        when(mapper.findUserByEmail("unknown@example.edu")).thenReturn(null);

        ResendVerificationResponse response = service.resendVerificationEmail(request);

        assertThat(response.message()).isEqualTo("인증 메일 재발송 요청이 접수되었습니다.");
        verify(mapper).insertMailAttemptStatus(null, "unknown@example.edu", "SKIPPED_UNKNOWN", "RESEND:signup-request");
        verify(mapper, never()).insertEmailVerificationToken(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(dispatcher, never()).sendVerificationMail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resendWithinOneMinuteReturnsRateLimitAndLeavesExistingTokenUnchanged() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test_pending@example.edu");
        when(mapper.findUserByEmail("test_pending@example.edu"))
                .thenReturn(new SignupMapper.SignupUserRow(79L, "test_pending", "test_pending@example.edu", "PENDING_EMAIL", "N"));
        when(mapper.countRecentResendAttempts("test_pending@example.edu")).thenReturn(1);

        assertThatThrownBy(() -> service.resendVerificationEmail(request))
                .isInstanceOf(CodedResponseException.class)
                .hasMessageContaining("인증 메일은 1분 후 다시 요청할 수 있습니다.");
        verify(mapper).insertMailAttemptStatus(79L, "test_pending@example.edu", "RATE_LIMITED", "RESEND:signup-request");
        verify(mapper, never()).invalidatePendingEmailVerificationTokens(79L);
        verify(mapper, never()).insertEmailVerificationToken(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(dispatcher, never()).sendVerificationMail(org.mockito.ArgumentMatchers.any());
    }
}
