package kr.ac.knue.commonfoundation.emailverification;

import java.time.LocalDateTime;

public record EmailVerificationEvent(
        Long eventId,
        Long userId,
        String eventType,
        LocalDateTime occurredAt,
        Long tokenId,
        String normalizedEmail,
        String actorType,
        String eventReason
) {
}
