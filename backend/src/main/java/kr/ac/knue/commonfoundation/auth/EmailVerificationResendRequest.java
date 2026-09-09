package kr.ac.knue.commonfoundation.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationResendRequest(
        @NotBlank(message = "이메일을 입력하세요.")
        @Pattern(regexp = "^\\s*[^@\\s]+@[^@\\s]+\\.[^@\\s]+\\s*$", message = "올바른 이메일 주소를 입력하세요.")
        @Size(max = 320, message = "이메일은 320자 이하로 입력하세요.")
        String email
) {
}
