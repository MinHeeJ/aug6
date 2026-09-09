package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationEventRepository;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationToken;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationTokenRepository;
import org.junit.jupiter.api.Test;

class EmailVerificationServiceTest {
    private static final String RAW_TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void validTokenAtomicallyUsesTokenActivatesPendingUserAndRecordsLinkEvent() {
        AuthMapper authMapper = org.mockito.Mockito.mock(AuthMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        EmailVerificationEventRepository events = org.mockito.Mockito.mock(EmailVerificationEventRepository.class);
        AuthService service = newService(authMapper, tokens, events);
        EmailVerificationToken token = activeToken("user@example.com", LocalDateTime.now().plusHours(1));
        when(tokens.findByRawToken(RAW_TOKEN)).thenReturn(token);
        when(authMapper.findVerificationUserById(77L)).thenReturn(pendingUser("user@example.com"));
        when(tokens.markUsedIfActive(900L)).thenReturn(1);
        when(authMapper.activatePendingEmailUser(77L)).thenReturn(1);

        EmailVerificationResponse response = service.updateEmailVerification(new EmailVerificationRequest(RAW_TOKEN));

        assertThat(response.accountStatus()).isEqualTo("ACTIVE");
        assertThat(response.emailVerifiedYn()).isEqualTo("Y");
        verify(tokens).markUsedIfActive(900L);
        verify(authMapper).activatePendingEmailUser(77L);
        verify(events).recordLinkVerified(77L, 900L, "user@example.com");
    }

    @Test
    void expiredTokenIsRejectedWithoutUsingTokenOrActivatingAccount() {
        AuthMapper authMapper = org.mockito.Mockito.mock(AuthMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        EmailVerificationEventRepository events = org.mockito.Mockito.mock(EmailVerificationEventRepository.class);
        AuthService service = newService(authMapper, tokens, events);
        EmailVerificationToken token = activeToken("user@example.com", LocalDateTime.now().minusMinutes(1));
        when(tokens.findByRawToken(RAW_TOKEN)).thenReturn(token);
        when(authMapper.findVerificationUserById(77L)).thenReturn(pendingUser("user@example.com"));

        assertThatThrownBy(() -> service.updateEmailVerification(new EmailVerificationRequest(RAW_TOKEN)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("만료");
        verify(tokens, never()).markUsedIfActive(900L);
        verify(authMapper, never()).activatePendingEmailUser(77L);
        verify(events).recordTokenRejected(77L, 900L, "user@example.com", "만료된 인증 링크입니다. 인증 메일 재발송을 요청하세요.");
    }

    @Test
    void usedOrSupersededTokenIsRejectedWithoutChangingAccount() {
        AuthMapper authMapper = org.mockito.Mockito.mock(AuthMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        EmailVerificationEventRepository events = org.mockito.Mockito.mock(EmailVerificationEventRepository.class);
        AuthService service = newService(authMapper, tokens, events);
        EmailVerificationToken token = tokenWithStatus("SUPERSEDED", "user@example.com", LocalDateTime.now().plusHours(1));
        when(tokens.findByRawToken(RAW_TOKEN)).thenReturn(token);
        when(authMapper.findVerificationUserById(77L)).thenReturn(pendingUser("user@example.com"));

        assertThatThrownBy(() -> service.updateEmailVerification(new EmailVerificationRequest(RAW_TOKEN)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("이전 링크");
        verify(tokens, never()).markUsedIfActive(900L);
        verify(authMapper, never()).activatePendingEmailUser(77L);
    }

    @Test
    void tamperedTokenIsRejectedWithoutAnyPersistenceChange() {
        AuthMapper authMapper = org.mockito.Mockito.mock(AuthMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        EmailVerificationEventRepository events = org.mockito.Mockito.mock(EmailVerificationEventRepository.class);
        AuthService service = newService(authMapper, tokens, events);
        when(tokens.findByRawToken(RAW_TOKEN)).thenReturn(null);

        assertThatThrownBy(() -> service.updateEmailVerification(new EmailVerificationRequest(RAW_TOKEN)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("유효하지 않은 인증 링크");
        verify(tokens, never()).markUsedIfActive(org.mockito.ArgumentMatchers.anyLong());
        verify(authMapper, never()).activatePendingEmailUser(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void validTokenForInactiveOrSuspendedAccountDoesNotReactivateUser() {
        AuthMapper authMapper = org.mockito.Mockito.mock(AuthMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        EmailVerificationEventRepository events = org.mockito.Mockito.mock(EmailVerificationEventRepository.class);
        AuthService service = newService(authMapper, tokens, events);
        EmailVerificationToken token = activeToken("user@example.com", LocalDateTime.now().plusHours(1));
        when(tokens.findByRawToken(RAW_TOKEN)).thenReturn(token);
        when(authMapper.findVerificationUserById(77L)).thenReturn(new AuthMapper.VerificationUserRow(
                77L, "newuser", "user@example.com", "INACTIVE", "N", "INACTIVE", "N"));

        assertThatThrownBy(() -> service.updateEmailVerification(new EmailVerificationRequest(RAW_TOKEN)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("활성화할 수 없습니다");
        verify(tokens, never()).markUsedIfActive(900L);
        verify(authMapper, never()).activatePendingEmailUser(77L);
    }

    private AuthService newService(AuthMapper authMapper, EmailVerificationTokenRepository tokens, EmailVerificationEventRepository events) {
        return new AuthService(
                org.mockito.Mockito.mock(AuthenticationPort.class),
                authMapper,
                org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class),
                org.mockito.Mockito.mock(SignupMapper.class),
                tokens,
                org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttemptRepository.class),
                org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.emailverification.VerificationMailSender.class),
                events
        );
    }

    private EmailVerificationToken activeToken(String email, LocalDateTime expiresAt) {
        return tokenWithStatus("ACTIVE", email, expiresAt);
    }

    private EmailVerificationToken tokenWithStatus(String status, String email, LocalDateTime expiresAt) {
        return new EmailVerificationToken(900L, 77L, email, "hash", LocalDateTime.now().minusMinutes(5), expiresAt, null, status, null, null);
    }

    private AuthMapper.VerificationUserRow pendingUser(String email) {
        return new AuthMapper.VerificationUserRow(77L, "newuser", email, "ACTIVE", "Y", "PENDING_EMAIL", "N");
    }
}
