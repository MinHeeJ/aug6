package kr.ac.knue.commonfoundation.emailverification;

import java.time.LocalDateTime;

public record EmailVerificationToken(
        Long tokenId,
        Long userId,
        String targetEmail,
        String tokenHash,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
