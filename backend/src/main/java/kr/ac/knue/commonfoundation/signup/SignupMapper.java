package kr.ac.knue.commonfoundation.signup;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SignupMapper {
    @Select("select count(1) from users where login_id = #{loginId}")
    int countByLoginId(@Param("loginId") String loginId);

    @Select("select count(1) from users where lower(email) = lower(#{email})")
    int countByEmail(@Param("email") String email);

    @Select("""
            select user_id,
                   login_id,
                   email,
                   account_status,
                   email_verified_yn
            from users
            where lower(email) = lower(#{email})
            """)
    @Results(id = "signupUserRow", value = {
            @Result(column = "user_id", property = "userId"),
            @Result(column = "login_id", property = "loginId"),
            @Result(column = "email", property = "email"),
            @Result(column = "account_status", property = "accountStatus"),
            @Result(column = "email_verified_yn", property = "emailVerifiedYn")
    })
    SignupUserRow findUserByEmail(@Param("email") String email);

    @Select("""
            select count(1)
            from mail_send_attempts msa
            where lower(msa.email) = lower(#{email})
              and msa.mail_type = 'EMAIL_VERIFICATION'
              and msa.send_status in ('REQUESTED','SENT','FAILED')
              and msa.reg_dt >= CURRENT_TIMESTAMP - INTERVAL '1 minute'
            """)
    int countRecentResendAttempts(@Param("email") String email);

    @Update("""
            update email_verification_tokens
            set used_yn = 'Y',
                used_dt = CURRENT_TIMESTAMP
            where user_id = #{userId}
              and used_yn = 'N'
              and used_dt is null
            """)
    int invalidatePendingEmailVerificationTokens(@Param("userId") Long userId);

    @Insert("""
            insert into mail_send_attempts (user_id, email, mail_type, send_status, request_id)
            values (#{userId}, lower(#{email}), 'EMAIL_VERIFICATION', #{sendStatus}, #{requestId})
            """)
    void insertMailAttemptStatus(@Param("userId") Long userId,
                                 @Param("email") String email,
                                 @Param("sendStatus") String sendStatus,
                                 @Param("requestId") String requestId);

    @Select("""
            insert into users (login_id, password_hash, email, email_verified_yn, account_status, system_use_yn, status, change_reason)
            values (#{loginId}, #{passwordHash}, #{email}, 'N', 'PENDING_EMAIL', 'Y', 'ACTIVE', 'BASIC57 signup')
            returning user_id
            """)
    Long insertPendingUser(@Param("loginId") String loginId, @Param("passwordHash") String passwordHash, @Param("email") String email);

    @Insert("""
            insert into user_roles (user_id, role_code, assignment_type, valid_start_date, status, change_reason)
            values (#{userId}, 'R08', 'MANUAL', CURRENT_DATE, 'ACTIVE', 'BASIC57 signup default R08')
            """)
    void insertDefaultRole(@Param("userId") Long userId);

    @Insert("""
            insert into email_verification_tokens (user_id, token_hash, expire_dt, used_yn, reg_dt)
            values (#{userId}, #{tokenHash}, CURRENT_TIMESTAMP + INTERVAL '24 hours', 'N', CURRENT_TIMESTAMP)
            """)
    void insertEmailVerificationToken(@Param("userId") Long userId, @Param("tokenHash") String tokenHash);

    @Select("""
            select evt.token_id,
                   evt.user_id,
                   evt.token_hash,
                   evt.expire_dt,
                   evt.used_yn,
                   evt.used_dt,
                   u.account_status,
                   u.email_verified_yn
            from email_verification_tokens evt
            join users u on u.user_id = evt.user_id
            where evt.token_hash = #{tokenHash}
            """)
    @Results(id = "emailVerificationTokenRow", value = {
            @Result(column = "token_id", property = "tokenId"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "token_hash", property = "tokenHash"),
            @Result(column = "expire_dt", property = "expireDt"),
            @Result(column = "used_yn", property = "usedYn"),
            @Result(column = "used_dt", property = "usedDt"),
            @Result(column = "account_status", property = "accountStatus"),
            @Result(column = "email_verified_yn", property = "emailVerifiedYn")
    })
    EmailVerificationTokenRow findEmailVerificationToken(@Param("tokenHash") String tokenHash);

    @Update("""
            update users
            set account_status = 'ACTIVE',
                email_verified_yn = 'Y',
                updated_at = CURRENT_TIMESTAMP,
                change_reason = 'BASIC57 email verification completed'
            where user_id = #{userId}
              and account_status = 'PENDING_EMAIL'
              and email_verified_yn = 'N'
            """)
    int activateVerifiedUser(@Param("userId") Long userId);

    @Update("""
            update email_verification_tokens
            set used_yn = 'Y',
                used_dt = CURRENT_TIMESTAMP
            where token_id = #{tokenId}
              and used_yn = 'N'
              and used_dt is null
            """)
    int markEmailVerificationTokenUsed(@Param("tokenId") Long tokenId);

    class SignupUserRow {
        private Long userId;
        private String loginId;
        private String email;
        private String accountStatus;
        private String emailVerifiedYn;

        public SignupUserRow() {
        }

        public SignupUserRow(Long userId, String loginId, String email, String accountStatus, String emailVerifiedYn) {
            this.userId = userId;
            this.loginId = loginId;
            this.email = email;
            this.accountStatus = accountStatus;
            this.emailVerifiedYn = emailVerifiedYn;
        }

        public Long userId() {
            return userId;
        }

        public String loginId() {
            return loginId;
        }

        public String email() {
            return email;
        }

        public String accountStatus() {
            return accountStatus;
        }

        public String emailVerifiedYn() {
            return emailVerifiedYn;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setAccountStatus(String accountStatus) {
            this.accountStatus = accountStatus;
        }

        public void setEmailVerifiedYn(String emailVerifiedYn) {
            this.emailVerifiedYn = emailVerifiedYn;
        }
    }

    class EmailVerificationTokenRow {
        private Long tokenId;
        private Long userId;
        private String tokenHash;
        private LocalDateTime expireDt;
        private String usedYn;
        private LocalDateTime usedDt;
        private String accountStatus;
        private String emailVerifiedYn;

        public EmailVerificationTokenRow() {
        }

        public EmailVerificationTokenRow(Long tokenId, Long userId, String tokenHash, LocalDateTime expireDt, String usedYn,
                                         LocalDateTime usedDt, String accountStatus, String emailVerifiedYn) {
            this.tokenId = tokenId;
            this.userId = userId;
            this.tokenHash = tokenHash;
            this.expireDt = expireDt;
            this.usedYn = usedYn;
            this.usedDt = usedDt;
            this.accountStatus = accountStatus;
            this.emailVerifiedYn = emailVerifiedYn;
        }

        public Long tokenId() {
            return tokenId;
        }

        public Long userId() {
            return userId;
        }

        public String tokenHash() {
            return tokenHash;
        }

        public LocalDateTime expireDt() {
            return expireDt;
        }

        public String usedYn() {
            return usedYn;
        }

        public LocalDateTime usedDt() {
            return usedDt;
        }

        public String accountStatus() {
            return accountStatus;
        }

        public String emailVerifiedYn() {
            return emailVerifiedYn;
        }

        public void setTokenId(Long tokenId) {
            this.tokenId = tokenId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public void setTokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
        }

        public void setExpireDt(LocalDateTime expireDt) {
            this.expireDt = expireDt;
        }

        public void setUsedYn(String usedYn) {
            this.usedYn = usedYn;
        }

        public void setUsedDt(LocalDateTime usedDt) {
            this.usedDt = usedDt;
        }

        public void setAccountStatus(String accountStatus) {
            this.accountStatus = accountStatus;
        }

        public void setEmailVerifiedYn(String emailVerifiedYn) {
            this.emailVerifiedYn = emailVerifiedYn;
        }
    }
}
