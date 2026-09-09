package kr.ac.knue.commonfoundation.emailverification;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailVerificationTokenMapper {
    @Insert("""
            insert into email_verification_tokens (user_id, target_email, token_hash, issued_at, expires_at, used_at, status)
            values (#{token.userId}, #{token.targetEmail}, #{token.tokenHash}, #{token.issuedAt}, #{token.expiresAt}, null, 'ACTIVE')
            """)
    int insertToken(@Param("token") NewEmailVerificationToken token);

    @Update("""
            update email_verification_tokens
            set status = 'SUPERSEDED', updated_at = CURRENT_TIMESTAMP
            where user_id = #{userId}
              and target_email = #{targetEmail}
              and status = 'ACTIVE'
            """)
    int supersedeActiveTokens(@Param("userId") Long userId, @Param("targetEmail") String targetEmail);

    @Select("""
            select token_id as "tokenId",
                   user_id as "userId",
                   target_email as "targetEmail",
                   token_hash as "tokenHash",
                   issued_at as "issuedAt",
                   expires_at as "expiresAt",
                   used_at as "usedAt",
                   status as "status",
                   created_at as "createdAt",
                   updated_at as "updatedAt"
            from email_verification_tokens
            where token_hash = #{tokenHash}
            """)
    EmailVerificationToken findByTokenHash(@Param("tokenHash") String tokenHash);

    @Select("""
            select token_id as "tokenId",
                   user_id as "userId",
                   target_email as "targetEmail",
                   token_hash as "tokenHash",
                   issued_at as "issuedAt",
                   expires_at as "expiresAt",
                   used_at as "usedAt",
                   status as "status",
                   created_at as "createdAt",
                   updated_at as "updatedAt"
            from email_verification_tokens
            where user_id = #{userId}
              and target_email = #{targetEmail}
              and status = 'ACTIVE'
            order by issued_at desc, token_id desc
            limit 1
            """)
    EmailVerificationToken findActiveToken(@Param("userId") Long userId, @Param("targetEmail") String targetEmail);

    @Update("""
            update email_verification_tokens
            set status = 'USED', used_at = #{usedAt}, updated_at = CURRENT_TIMESTAMP
            where token_id = #{tokenId}
              and status = 'ACTIVE'
              and used_at is null
              and expires_at > #{usedAt}
            """)
    int markUsedIfActive(@Param("tokenId") Long tokenId, @Param("usedAt") LocalDateTime usedAt);

    @Update("""
            update email_verification_tokens
            set status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP
            where status = 'ACTIVE'
              and expires_at <= #{now}
            """)
    int expireActiveTokens(@Param("now") LocalDateTime now);

    record NewEmailVerificationToken(
            Long userId,
            String targetEmail,
            String tokenHash,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
    }
}
