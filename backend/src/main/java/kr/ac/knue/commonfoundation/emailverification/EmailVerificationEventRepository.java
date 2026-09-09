package kr.ac.knue.commonfoundation.emailverification;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EmailVerificationEventRepository {
    private final EmailVerificationEventMapper mapper;

    public EmailVerificationEventRepository(EmailVerificationEventMapper mapper) {
        this.mapper = mapper;
    }

    public void recordExistingAccountExemption(Long userId, String email) {
        mapper.insertEvent(userId, "EXEMPT_EXISTING", null, nullableNormalizedEmail(email), "SYSTEM", "기존 계정 이메일 인증 정책 면제");
    }

    public void recordLinkVerified(Long userId, Long tokenId, String email) {
        mapper.insertEvent(userId, "LINK_VERIFIED", tokenId, EmailAddressPolicy.normalizeEmail(email), "VISITOR", "이메일 인증 링크 확인 완료");
    }

    public void recordResendRequested(Long userId, String email) {
        mapper.insertEvent(userId, "RESEND_REQUESTED", null, EmailAddressPolicy.normalizeEmail(email), "VISITOR", "이메일 인증 재발송 요청");
    }

    public void recordTokenRejected(Long userId, Long tokenId, String email, String reason) {
        mapper.insertEvent(userId, "TOKEN_REJECTED", tokenId, nullableNormalizedEmail(email), "VISITOR", reason);
    }

    public List<EmailVerificationEvent> findByUserId(Long userId) {
        return mapper.findByUserId(userId);
    }

    public boolean hasEvent(Long userId, String eventType) {
        return mapper.countByUserAndType(userId, eventType) > 0;
    }

    private String nullableNormalizedEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return EmailAddressPolicy.normalizeEmail(email);
    }
}
