package kr.ac.knue.commonfoundation.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
        @NotBlank(message = "인증 토큰을 입력하세요.")
        @Size(max = 512, message = "인증 토큰 형식이 올바르지 않습니다.")
        @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "인증 토큰 형식이 올바르지 않습니다.")
        String token
) {
}
