package kr.ac.knue.commonfoundation.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 100, message = "로그인 ID는 100자 이하로 입력하세요.")
        String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 255, message = "비밀번호는 255자 이하로 입력하세요.")
        String password,
        @NotBlank(message = "이메일은 필수입니다.")
        @Size(max = 320, message = "이메일은 320자 이하로 입력하세요.")
        String email
) {
}
