package kr.ac.knue.commonfoundation.signup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.ac.knue.commonfoundation.common.api.CodedResponseException;
import kr.ac.knue.commonfoundation.mail.EmailVerificationMailDispatcher;
import kr.ac.knue.commonfoundation.mail.VerificationMailCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SignupServiceTest {
    SignupBoundaryPolicy policy = new SignupBoundaryPolicy();
    SignupMapper mapper = org.mockito.Mockito.mock(SignupMapper.class);
    PasswordHashService passwordHashService = org.mockito.Mockito.mock(PasswordHashService.class);
    SignupTokenService tokenService = org.mockito.Mockito.mock(SignupTokenService.class);
    EmailVerificationMailDispatcher dispatcher = org.mockito.Mockito.mock(EmailVerificationMailDispatcher.class);
    SignupService service = new SignupService(policy, mapper, passwordHashService, tokenService, dispatcher);

    @Test
    void successfulSignupPersistsPendingUserRoleHashedTokenAndDispatchesMailWithoutPasswordLeak() {
        SignupRequest request = request("signupuser01", "Strong!123", "Strong!123", "SignupUser01@Example.Test");
        when(mapper.countByLoginId("signupuser01")).thenReturn(0);
        when(mapper.countByEmail("signupuser01@example.test")).thenReturn(0);
        when(passwordHashService.hash("Strong!123")).thenReturn("$argon2id$v=19$m=4096,t=3,p=1$hash");
        when(tokenService.generateRawToken()).thenReturn("raw-token-with-more-than-thirty-two-url-safe-bytes");
        when(tokenService.sha256Hex("raw-token-with-more-than-thirty-two-url-safe-bytes")).thenReturn("a".repeat(64));
        when(mapper.insertPendingUser("signupuser01", "$argon2id$v=19$m=4096,t=3,p=1$hash", "signupuser01@example.test"))
                .thenReturn(77L);

        SignupResponse response = service.createSignup(request);

        assertThat(response.accountStatus()).isEqualTo("PENDING_EMAIL");
        assertThat(response.email()).isEqualTo("signupuser01@example.test");
        assertThat(response.toString()).doesNotContain("Strong!123");
        verify(mapper).insertDefaultRole(77L);
        verify(mapper).insertEmailVerificationToken(77L, "a".repeat(64));
        ArgumentCaptor<VerificationMailCommand> mail = ArgumentCaptor.forClass(VerificationMailCommand.class);
        verify(dispatcher).sendVerificationMail(mail.capture());
        assertThat(mail.getValue().verificationToken()).isEqualTo("raw-token-with-more-than-thirty-two-url-safe-bytes");
        assertThat(mail.getValue().email()).isEqualTo("signupuser01@example.test");
    }

    @Test
    void duplicateEmailStopsPersistenceAndTokenCreation() {
        SignupRequest request = request("signupuser01", "Strong!123", "Strong!123", "dup@example.test");
        when(mapper.countByLoginId("signupuser01")).thenReturn(0);
        when(mapper.countByEmail("dup@example.test")).thenReturn(1);

        assertThatThrownBy(() -> service.createSignup(request))
                .isInstanceOf(CodedResponseException.class)
                .hasMessageContaining("이미 등록된 이메일입니다.");
        verify(mapper, never()).insertPendingUser(any(), any(), any());
        verify(dispatcher, never()).sendVerificationMail(any());
    }

    private SignupRequest request(String loginId, String password, String confirm, String email) {
        SignupRequest request = new SignupRequest();
        request.setLoginId(loginId);
        request.setPassword(password);
        request.setPasswordConfirm(confirm);
        request.setEmail(email);
        return request;
    }
}
