package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class SignupServiceTest {
    @Test
    void springCanConstructSignupServiceWithItsMapperDependency() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SignupMapper.class, () -> mock(SignupMapper.class));
            context.register(SignupService.class);
            context.refresh();

            assertThat(context.getBean(SignupService.class)).isNotNull();
        }
    }

    @Test
    void signupNormalizesValuesStoresArgon2idHashAndAssignsR01() {
        SignupMapper mapper = mock(SignupMapper.class);
        SignupService service = new SignupService(mapper);
        when(mapper.countByLoginId("newuser1")).thenReturn(0);
        when(mapper.countByEmail("newuser1@example.com")).thenReturn(0);
        when(mapper.insertUser(eq("newuser1"), any(String.class), eq("newuser1@example.com")))
                .thenReturn(501L);
        when(mapper.insertDefaultRole(501L)).thenReturn(1);

        SignupResponse response = service.signup(new SignupRequest(
                "NewUser1",
                "Abcd!234",
                "Abcd!234",
                "NEWUSER1@EXAMPLE.COM"));

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertUser(eq("newuser1"), passwordHash.capture(), eq("newuser1@example.com"));
        verify(mapper).insertDefaultRole(501L);
        assertThat(passwordHash.getValue()).startsWith("$argon2id$").doesNotContain("Abcd!234");
        assertThat(response).isEqualTo(new SignupResponse("newuser1", "가입이 완료되었습니다."));
    }

    @Test
    void signupRejectsPasswordConfirmationBeforeAnyWrite() {
        SignupMapper mapper = mock(SignupMapper.class);
        SignupService service = new SignupService(mapper);
        when(mapper.countByLoginId("newuser1")).thenReturn(0);
        when(mapper.countByEmail("newuser1@example.com")).thenReturn(0);

        assertThatThrownBy(() -> service.signup(new SignupRequest(
                "newuser1",
                "Abcd!234",
                "Other!234",
                "newuser1@example.com")))
                .hasMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");

        verify(mapper, never()).insertUser(any(), any(), any());
        verify(mapper, never()).insertDefaultRole(any());
    }

    @Test
    void signupFailsWhenDefaultRoleWriteDoesNotAffectOneRow() {
        SignupMapper mapper = mock(SignupMapper.class);
        SignupService service = new SignupService(mapper);
        when(mapper.countByLoginId("newuser1")).thenReturn(0);
        when(mapper.countByEmail("newuser1@example.com")).thenReturn(0);
        when(mapper.insertUser(any(), any(), any())).thenReturn(501L);
        when(mapper.insertDefaultRole(501L)).thenReturn(0);

        assertThatThrownBy(() -> service.signup(new SignupRequest(
                "newuser1",
                "Abcd!234",
                "Abcd!234",
                "newuser1@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("기본 역할을 부여하지 못했습니다.");
    }

    @Test
    void availabilityUsesNormalizedIdentifierAndDoesNotWrite() {
        SignupMapper mapper = mock(SignupMapper.class);
        SignupService service = new SignupService(mapper);
        when(mapper.countByLoginId("newuser1")).thenReturn(1);

        UserIdAvailabilityResponse response = service.checkUserIdAvailability("NEWUSER1");

        assertThat(response.available()).isFalse();
        verify(mapper, never()).insertUser(any(), any(), any());
    }

    @Test
    void availabilityWithoutAnIdentifierReturnsUnavailableWithoutQueryingAccounts() {
        SignupMapper mapper = mock(SignupMapper.class);
        SignupService service = new SignupService(mapper);

        UserIdAvailabilityResponse response = service.checkUserIdAvailability(null);

        assertThat(response.available()).isFalse();
        verify(mapper, never()).countByLoginId(any());
    }
}
