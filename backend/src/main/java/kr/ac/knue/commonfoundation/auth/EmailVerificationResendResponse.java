package kr.ac.knue.commonfoundation.auth;

public record EmailVerificationResendResponse(String status, String message) {
    public static EmailVerificationResendResponse accepted() {
        return new EmailVerificationResendResponse("ACCEPTED", "인증 메일 재발송 요청을 접수했습니다.");
    }
}
