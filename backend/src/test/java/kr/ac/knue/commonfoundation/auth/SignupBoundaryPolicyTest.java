package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;

import kr.ac.knue.commonfoundation.signup.SignupBoundaryPolicy;
import kr.ac.knue.commonfoundation.signup.SignupBoundaryPolicy.ValidationResult;
import org.junit.jupiter.api.Test;

class SignupBoundaryPolicyTest {
    private final SignupBoundaryPolicy policy = new SignupBoundaryPolicy();

    @Test
    void rejectsWeakPasswordBeforePersistenceOrMailDispatch() {
        ValidationResult result = policy.validateSignupBoundary("user01", "weak", "weak", "user01@knue.ac.kr");

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo("WEAK_PASSWORD");
        assertThat(result.field()).isEqualTo("password");
        assertThat(result.message()).doesNotContain("weak");
    }

    @Test
    void rejectsPasswordMismatchAndInvalidEmailWithStableErrorCodes() {
        ValidationResult mismatch = policy.validateSignupBoundary("user01", "Strong!123", "Strong!124", "user01@knue.ac.kr");
        ValidationResult invalidEmail = policy.validateSignupBoundary("user01", "Strong!123", "Strong!123", "not-an-email");

        assertThat(mismatch.errorCode()).isEqualTo("PASSWORD_MISMATCH");
        assertThat(mismatch.field()).isEqualTo("passwordConfirm");
        assertThat(invalidEmail.errorCode()).isEqualTo("INVALID_EMAIL");
        assertThat(invalidEmail.field()).isEqualTo("email");
    }

    @Test
    void acceptsComplexPasswordAndNormalizesEmailWithoutLeakingSecrets() {
        ValidationResult result = policy.validateSignupBoundary("user01", "Strong!123", "Strong!123", "USER01@KNUE.AC.KR");

        assertThat(result.valid()).isTrue();
        assertThat(policy.normalizeEmail("USER01@KNUE.AC.KR")).isEqualTo("user01@knue.ac.kr");
        assertThat(result.toString()).doesNotContain("Strong!123");
    }

    @Test
    void asyncMailBoundaryKeepsSignupResponseBudgetBelowFiveSeconds() {
        assertThat(SignupBoundaryPolicy.SIGNUP_RESPONSE_BUDGET).isLessThan(java.time.Duration.ofSeconds(5));
    }
}
