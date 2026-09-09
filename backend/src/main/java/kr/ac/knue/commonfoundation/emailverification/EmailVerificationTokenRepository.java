package kr.ac.knue.commonfoundation.emailverification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EmailVerificationTokenRepository {
    private static final int TOKEN_BYTES = 32;
    private static final String PENDING_EMAIL = "PENDING_EMAIL";
    private static final String LINK_VERIFIED = "LINK_VERIFIED";
    private static final String EMAIL_VERIFIED_YN = "email_verified_yn";

    private final EmailVerificationTokenMapper mapper;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public EmailVerificationTokenRepository(EmailVerificationTokenMapper mapper) {
        this(mapper, new SecureRandom(), Clock.systemUTC());
    }

    EmailVerificationTokenRepository(EmailVerificationTokenMapper mapper, SecureRandom secureRandom, Clock clock) {
        this.mapper = mapper;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public IssuedEmailVerificationToken issueReplacementToken(Long userId, String targetEmail) {
        String normalizedEmail = EmailAddressPolicy.normalizeEmail(targetEmail);
        mapper.supersedeActiveTokens(userId, normalizedEmail);
        LocalDateTime issuedAt = LocalDateTime.now(clock);
        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);
        mapper.insertToken(new EmailVerificationTokenMapper.NewEmailVerificationToken(
                userId,
                normalizedEmail,
                tokenHash,
                issuedAt,
                issuedAt.plusHours(24)
        ));
        return new IssuedEmailVerificationToken(rawToken, tokenHash, normalizedEmail, issuedAt.plusHours(24));
    }

    public EmailVerificationToken findByRawToken(String rawToken) {
        return mapper.findByTokenHash(sha256(rawToken));
    }

    public EmailVerificationToken findByHash(String tokenHash) {
        return mapper.findByTokenHash(tokenHash);
    }

    public EmailVerificationToken findActiveToken(Long userId, String targetEmail) {
        return mapper.findActiveToken(userId, EmailAddressPolicy.normalizeEmail(targetEmail));
    }

    public int markUsedIfActive(Long tokenId) {
        return mapper.markUsedIfActive(tokenId, LocalDateTime.now(clock));
    }

    public int expireActiveTokens() {
        return mapper.expireActiveTokens(LocalDateTime.now(clock));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }

    public record IssuedEmailVerificationToken(
            String rawToken,
            String tokenHash,
            String targetEmail,
            LocalDateTime expiresAt
    ) {
    }
}
