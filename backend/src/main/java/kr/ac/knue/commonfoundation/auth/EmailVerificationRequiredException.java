package kr.ac.knue.commonfoundation.auth;

import kr.ac.knue.commonfoundation.common.api.ApiError;

public class EmailVerificationRequiredException extends RuntimeException {
    private final ApiError apiError;

    public EmailVerificationRequiredException() {
        super("이메일 인증이 필요합니다. /api/auth/email-verifications/resend로 인증 메일 재발송을 요청하세요.");
        this.apiError = new ApiError(
                "EMAIL_VERIFICATION_REQUIRED",
                getMessage(),
                java.util.List.of()
        );
    }

    public ApiError apiError() {
        return apiError;
    }
}
