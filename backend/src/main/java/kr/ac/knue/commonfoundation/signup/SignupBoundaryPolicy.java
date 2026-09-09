package kr.ac.knue.commonfoundation.signup;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SignupBoundaryPolicy {
    public static final Duration SIGNUP_RESPONSE_BUDGET = Duration.ofMillis(4500);
    private static final Pattern LOGIN_ID = Pattern.compile("^[a-z][a-z0-9]{3,19}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public ValidationResult validateSignupBoundary(String loginId, String password, String passwordConfirm, String email) {
        ValidationResult loginIdResult = validateLoginId(loginId);
        if (!loginIdResult.valid()) {
            return loginIdResult;
        }
        ValidationResult emailResult = validateEmail(email);
        if (!emailResult.valid()) {
            return emailResult;
        }
        if (isBlank(password)) {
            return ValidationResult.invalid("FIELD_REQUIRED", "password", "비밀번호는 필수입니다.");
        }
        if (isBlank(passwordConfirm)) {
            return ValidationResult.invalid("FIELD_REQUIRED", "passwordConfirm", "비밀번호 확인은 필수입니다.");
        }
        if (!password.equals(passwordConfirm)) {
            return ValidationResult.invalid("PASSWORD_MISMATCH", "passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (isWeakPassword(loginId, password)) {
            return ValidationResult.invalid("WEAK_PASSWORD", "password", "비밀번호는 8자 이상, 영문 대소문자·숫자·특수문자 중 3종 이상을 포함해야 합니다.");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateLoginId(String loginId) {
        if (isBlank(loginId)) {
            return ValidationResult.invalid("FIELD_REQUIRED", "loginId", "아이디는 필수입니다.");
        }
        if (!LOGIN_ID.matcher(loginId.trim()).matches()) {
            return ValidationResult.invalid("INVALID_LOGIN_ID", "loginId", "아이디는 영문 소문자로 시작하고 영문 소문자와 숫자 4~20자로 입력해주세요.");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateEmail(String email) {
        if (isBlank(email)) {
            return ValidationResult.invalid("FIELD_REQUIRED", "email", "이메일은 필수입니다.");
        }
        if (!EMAIL.matcher(email.trim()).matches() || email.trim().length() > 254) {
            return ValidationResult.invalid("INVALID_EMAIL", "email", "올바른 이메일 형식이 아닙니다.");
        }
        return ValidationResult.ok();
    }

    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isWeakPassword(String loginId, String password) {
        if (password.length() < 8 || password.equals(loginId) || hasTripleRepeatedCharacter(password)) {
            return true;
        }
        int categories = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isLowerCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            categories++;
        }
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            categories++;
        }
        return categories < 3;
    }

    private boolean hasTripleRepeatedCharacter(String password) {
        for (int i = 2; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1) && password.charAt(i) == password.charAt(i - 2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    public record ValidationResult(boolean valid, String errorCode, String field, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null, null, null);
        }

        public static ValidationResult invalid(String errorCode, String field, String message) {
            return new ValidationResult(false, errorCode, field, message);
        }
    }
}
