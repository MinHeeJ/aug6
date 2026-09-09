package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.ac.knue.commonfoundation.emailverification.EmailVerificationEventRepository;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationTokenRepository;
import kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttempt;
import kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttemptRepository;
import kr.ac.knue.commonfoundation.emailverification.VerificationMailDeliveryException;
import kr.ac.knue.commonfoundation.emailverification.VerificationMailSender;
import org.junit.jupiter.api.Test;

class SignupServiceIntegrationTest {
    @Test
    void successfulSignupCreatesPendingEmailUserPasswordHashTokenAndGeneralRoleOnly() {
        SignupMapper mapper = org.mockito.Mockito.mock(SignupMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        MailDeliveryAttemptRepository attempts = org.mockito.Mockito.mock(MailDeliveryAttemptRepository.class);
        VerificationMailSender mailSender = org.mockito.Mockito.mock(VerificationMailSender.class);
        AuthService service = new AuthService(
                org.mockito.Mockito.mock(AuthenticationPort.class),
                org.mockito.Mockito.mock(AuthMapper.class),
                org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class),
                mapper,
                tokens,
                attempts,
                mailSender,
                org.mockito.Mockito.mock(EmailVerificationEventRepository.class)
        );
        when(mapper.countByLoginId("newuser")).thenReturn(0);
        when(mapper.countNormalUsersByEmail("user@example.com")).thenReturn(0);
        when(mapper.findUserIdByLoginId("newuser")).thenReturn(99L);
        when(attempts.recordRequested(99L, "user@example.com")).thenReturn(10L);
        when(attempts.findById(10L)).thenReturn(new MailDeliveryAttempt(10L, 99L, "user@example.com", "SENT", "NONE", null, "N", null, null, null, null));
        when(tokens.issueReplacementToken(eq(99L), eq("user@example.com")))
                .thenReturn(new EmailVerificationTokenRepository.IssuedEmailVerificationToken("raw", "hash", "user@example.com", null));

        SignupResponse response = service.signup(new SignupRequest(" newuser ", "Password1", " USER@Example.COM "));

        assertThat(response.accountStatus()).isEqualTo("PENDING_EMAIL");
        assertThat(response.emailVerifiedYn()).isEqualTo("N");
        verify(mapper).insertPendingSignupUser(any(SignupMapper.NewSignupUser.class));
        verify(mapper).assignDefaultGeneralUserRole(99L);
        verify(mapper, never()).assignRole(eq(99L), eq("R09"));
        verify(tokens).issueReplacementToken(99L, "user@example.com");
        verify(mailSender).sendVerificationMail("user@example.com", "raw");
    }

    @Test
    void gmailSendFailureLeavesPendingEmailAndRecordsRetryableFailureWithoutMailhogFallback() {
        SignupMapper mapper = org.mockito.Mockito.mock(SignupMapper.class);
        EmailVerificationTokenRepository tokens = org.mockito.Mockito.mock(EmailVerificationTokenRepository.class);
        MailDeliveryAttemptRepository attempts = org.mockito.Mockito.mock(MailDeliveryAttemptRepository.class);
        VerificationMailSender mailSender = org.mockito.Mockito.mock(VerificationMailSender.class);
        AuthService service = new AuthService(
                org.mockito.Mockito.mock(AuthenticationPort.class),
                org.mockito.Mockito.mock(AuthMapper.class),
                org.mockito.Mockito.mock(kr.ac.knue.commonfoundation.permissions.EffectivePermissionService.class),
                mapper,
                tokens,
                attempts,
                mailSender,
                org.mockito.Mockito.mock(EmailVerificationEventRepository.class)
        );
        when(mapper.countByLoginId("newuser")).thenReturn(0);
        when(mapper.countNormalUsersByEmail("user@example.com")).thenReturn(0);
        when(mapper.findUserIdByLoginId("newuser")).thenReturn(99L);
        when(attempts.recordRequested(99L, "user@example.com")).thenReturn(11L);
        when(attempts.findById(11L)).thenReturn(new MailDeliveryAttempt(11L, 99L, "user@example.com", "FAILED_OTHER", "CONFIGURATION", "SMTP 설정 미완료", "Y", null, null, null, null));
        when(tokens.issueReplacementToken(eq(99L), eq("user@example.com")))
                .thenReturn(new EmailVerificationTokenRepository.IssuedEmailVerificationToken("raw", "hash", "user@example.com", null));
        org.mockito.Mockito.doThrow(new VerificationMailDeliveryException("FAILED_OTHER", "CONFIGURATION", "SMTP 설정 미완료", true))
                .when(mailSender).sendVerificationMail("user@example.com", "raw");

        SignupResponse response = service.signup(new SignupRequest("newuser", "Password1", "user@example.com"));

        assertThat(response.accountStatus()).isEqualTo("PENDING_EMAIL");
        assertThat(response.emailVerifiedYn()).isEqualTo("N");
        assertThat(response.mailDeliveryStatus()).isEqualTo("FAILED_OTHER");
        verify(attempts).markFailure(11L, "FAILED_OTHER", "CONFIGURATION", "SMTP 설정 미완료", true);
        verify(mailSender, never()).sendMailhogFallback(any(), any());
    }
}
