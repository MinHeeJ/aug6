package kr.ac.knue.commonfoundation.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public account-registration input. Password fields are accepted only for validation and hashing.
 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[a-z][a-z0-9]{3,19}$", message = "아이디는 영문 소문자로 시작하는 영문 소문자와 숫자만 사용할 수 있습니다.")
        String userId,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,
        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        @Size(min = 8, message = "비밀번호 확인은 8자 이상이어야 합니다.")
        String passwordConfirm,
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
        String email) {
}
