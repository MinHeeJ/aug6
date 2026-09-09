package kr.ac.knue.commonfoundation.auth;

public record EmailVerificationResponse(
        Long userId,
        String loginId,
        String email,
        String accountStatus,
        String emailVerifiedYn
) {
}
