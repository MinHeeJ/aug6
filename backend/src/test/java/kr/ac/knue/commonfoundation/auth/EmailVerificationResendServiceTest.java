package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.TooManyRequestsException;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationEventRepository;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationTokenRepository;
import kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttempt;
import kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttemptRepository;
import kr.ac.knue.commonfoundation.emailverification.VerificationMailDeliveryException;
import kr.ac.knue.commonfoundation.emailverification.VerificationMailSender;
import org.junit.jupiter.api.Test;

class EmailVerificationResendServiceTest {
    @Test
    void pendingUserAfterThrottleWindowInvalidatesOldTokenCreatesNewAttemptAndSendsMail() {
        Fixture fixture = new Fixture();
        when(fixture.signupMapper.findResendTargetByEmail("user@example.com"))
                .thenReturn(pendingTarget());
        when(fixture.attempts.hasRecentAttemptInsideThrottleWindow(77L, "user@example.com", 60)).thenReturn(false);
        when(fixture.attempts.countAttemptsSince("user@example.com", null, 3600)).thenReturn(0);
        when(fixture.attempts.countAttemptsSince(null, "203.0.113.10", 3600)).thenReturn(0);
        when(fixture.tokens.issueReplacementToken(77L, "user@example.com"))
                .thenReturn(new EmailVerificationTokenRepository.IssuedEmailVerificationToken("raw-token", "hash", "user@example.com", null));
        when(fixture.attempts.recordRequested(77L, "user@example.com", "203.0.113.10")).thenReturn(501L);

        EmailVerificationResendResponse response = fixture.service.createEmailVerificationResend(
                new EmailVerificationResendRequest(" USER@Example.COM "), "203.0.113.10");

        assertThat(response.status()).isEqualTo("ACCEPTED");
        verify(fixture.tokens).issueReplacementToken(77L, "user@example.com");
        verify(fixture.attempts).recordRequested(77L, "user@example.com", "203.0.113.10");
        verify(fixture.mailSender).sendVerificationMail("user@example.com", "raw-token");
        verify(fixture.attempts).markSent(501L);
        verify(fixture.events).recordResendRequested(77L, "user@example.com");
    }

    @Test
    void sameAccountOrEmailWithinSixtySecondsRejectsWithoutReplacingToken() {
        Fixture fixture = new Fixture();
        when(fixture.signupMapper.findResendTargetByEmail("user@example.com"))
                .thenReturn(pendingTarget());
        when(fixture.attempts.hasRecentAttemptInsideThrottleWindow(77L, "user@example.com", 60)).thenReturn(true);

        assertThatThrownBy(() -> fixture.service.createEmailVerificationResend(
                new EmailVerificationResendRequest("user@example.com"), "203.0.113.10"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("60초");
        verify(fixture.tokens, never()).issueReplacementToken(any(), any());
        verify(fixture.attempts, never()).recordRequested(any(), any(), any());
    }

    @Test
    void configurableEmailAndIpLimitsRejectBeforeTokenReplacement() {
        Fixture fixture = new Fixture(new EmailVerificationResendProperties(60, 2, 1));
        when(fixture.signupMapper.findResendTargetByEmail("user@example.com"))
                .thenReturn(pendingTarget());
        when(fixture.attempts.hasRecentAttemptInsideThrottleWindow(77L, "user@example.com", 60)).thenReturn(false);
        when(fixture.attempts.countAttemptsSince("user@example.com", null, 3600)).thenReturn(2);

        assertThatThrownBy(() -> fixture.service.createEmailVerificationResend(
                new EmailVerificationResendRequest("user@example.com"), "203.0.113.10"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(fixture.tokens, never()).issueReplacementToken(any(), any());

        Fixture ipFixture = new Fixture(new EmailVerificationResendProperties(60, 10, 1));
        when(ipFixture.signupMapper.findResendTargetByEmail("user@example.com"))
                .thenReturn(pendingTarget());
        when(ipFixture.attempts.hasRecentAttemptInsideThrottleWindow(77L, "user@example.com", 60)).thenReturn(false);
        when(ipFixture.attempts.countAttemptsSince("user@example.com", null, 3600)).thenReturn(0);
        when(ipFixture.attempts.countAttemptsSince(null, "203.0.113.10", 3600)).thenReturn(1);

        assertThatThrownBy(() -> ipFixture.service.createEmailVerificationResend(
                new EmailVerificationResendRequest("user@example.com"), "203.0.113.10"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(ipFixture.tokens, never()).issueReplacementToken(any(), any());
    }

    @Test
    void verifiedExistingExemptUnknownAndSubstituteEmailsReceiveNeutralResponseWithoutMail() {
        Fixture fixture = new Fixture();
        when(fixture.signupMapper.findResendTargetByEmail("admin@example.com"))
                .thenReturn(new SignupMapper.ResendTargetUser(1L, "admin", "admin@example.com", "ACTIVE", "Y", "EXEMPT_EXISTING", "N", "ACTIVE"));
        when(fixture.signupMapper.findResendTargetByEmail("unknown@example.com")).thenReturn(null);
        when(fixture.signupMapper.findResendTargetByEmail("admin@kndadmin.com"))
                .thenReturn(new SignupMapper.ResendTargetUser(2L, "legacy", "admin@kndadmin.com", "ACTIVE", "Y", "EXEMPT_EXISTING", "Y", "ACTIVE"));

        assertThat(fixture.service.createEmailVerificationResend(new EmailVerificationResendRequest("admin@example.com"), "203.0.113.10").status()).isEqualTo("ACCEPTED");
        assertThat(fixture.service.createEmailVerificationResend(new EmailVerificationResendRequest("unknown@example.com"), "203.0.113.10").status()).isEqualTo("ACCEPTED");
        assertThat(fixture.service.createEmailVerificationResend(new EmailVerificationResendRequest("admin@kndadmin.com"), "203.0.113.10").status()).isEqualTo("ACCEPTED");

        verify(fixture.tokens, never()).issueReplacementToken(any(), any());
        verify(fixture.attempts, never()).recordRequested(any(), any(), any());
        verify(fixture.mailSender, never()).sendVerificationMail(any(), any());
    }

    @Test
    void gmailAuthTimeoutAndRateLimitFailuresAreRecordedAsDistinctOperatorDiagnostics() {
        assertDeliveryFailureRecorded("FAILED_AUTH", "AUTH");
        assertDeliveryFailureRecorded("FAILED_TIMEOUT", "TIMEOUT");
        assertDeliveryFailureRecorded("FAILED_RATE_LIMIT", "RATE_LIMIT");
    }

    @Test
    void diagnosticsAreRedactedBeforePersistence() {
        Fixture fixture = new Fixture();
        when(fixture.signupMapper.findResendTargetByEmail("user@example.com")).thenReturn(pendingTarget());
        when(fixture.attempts.hasRecentAttemptInsideThrottleWindow(77L, "user@example.com", 60)).thenReturn(false);
        when(fixture.attempts.countAttemptsSince("user@example.com", null, 3600)).thenReturn(0);
        when(fixture.attempts.countAttemptsSince(null, "203.0.113.10", 3600)).thenReturn(0);
        when(fixture.tokens.issueReplacementToken(77L, "user@example.com"))
                .thenReturn(new EmailVerificationTokenRepository.IssuedEmailVerificationToken("super-secret-token", "hash", "user@example.com", null));
        when(fixture.attempts.recordRequested(77L, "user@example.com", "203.0.113.10")).thenReturn(501L);
        org.mockito.Mockito.doThrow(new VerificationMailDeliveryException("FAILED_AUTH", "AUTH", "password=abc token=super-secret-token", true))
                .when(fixture.mailSender).sendVerificationMail("user@example.com", "super-secret-token");

        fixture.service.createEmailVerificationResend(new EmailVerificationResendRequest("user@example.com"), "203.0.113.10");

        verify(fixture.attempts).markFailure(501L, "FAILED_AUTH", "AUTH", "password=REDACTED token=REDACTED", true);
    }

    private void assertDeliveryFailureRecorded(String status, String category) {
        Fixture fixture = new Fixture();
        when(fixture.signupMapper.findResendTargetByEmail("user@example.com")).thenReturn(pendingTarget());
        when(fixture.attempts.hasRecentAttemptInsideThrottleWindow(77L, "user@example.com", 60)).thenReturn(false);
        when(fixture.attempts.countAttemptsSince("user@example.com", null, 3600)).thenReturn(0);
        when(fixture.attempts.countAttemptsSince(null, "203.0.113.10", 3600)).thenReturn(0);
        when(fixture.tokens.issueReplacementToken(77L, "user@example.com"))
                .thenReturn(new EmailVerificationTokenRepository.IssuedEmailVerificationToken("raw", "hash", "user@example.com", null));
        when(fixture.attempts.recordRequested(77L, "user@example.com", "203.0.113.10")).thenReturn(501L);
        org.mockito.Mockito.doThrow(new VerificationMailDeliveryException(status, category, category + " failure", true))
                .when(fixture.mailSender).sendVerificationMail("user@example.com", "raw");

        fixture.service.createEmailVerificationResend(new EmailVerificationResendRequest("user@example.com"), "203.0.113.10");

        verify(fixture.attempts).markFailure(501L, status, category, category + " failure", true);
    }

    private static SignupMapper.ResendTargetUser pendingTarget() {
        return new SignupMapper.ResendTargetUser(77L, "newuser", "user@example.com", "PENDING_EMAIL", "N", null, "N", "ACTIVE");
    }

    private static class Fixture {
        final SignupMapper signupMapper = org.mockito.Mockito.mock(SignupMapper.class);
        final EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        final MailDeliveryAttemptRepository attempts = org.mockito.Mockito.mock(MailDeliveryAttemptRepository.class);
        final VerificationMailSender mailSender = org.mockito.Mockito.mock(VerificationMailSender.class);
        final EmailVerificationEventRepository events = org.mockito.Mockito.mock(EmailVerificationEventRepository.class);
        final AuthService service;

        Fixture() {
            this(new EmailVerificationResendProperties(60, 5, 20));
        }

        Fixture(EmailVerificationResendProperties properties) {
            service = new AuthService(
                    org.mockito.Mockito.mock(AuthenticationPort.class),
                    org.mockito.Mockito.mock(AuthMapper.class),
                    org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class),
                    signupMapper,
                    tokens,
                    attempts,
                    mailSender,
                    events,
                    properties
            );
        }
    }
}
