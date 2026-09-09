package kr.ac.knue.commonfoundation.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SignupMapper {
    @Select("select count(*) from users where lower(login_id) = lower(#{loginId})")
    int countByLoginId(@Param("loginId") String loginId);

    @Select("""
            select count(*)
            from users
            where email is not null
              and coalesce(email_substitute_yn, 'N') = 'N'
              and lower(email) = #{email}
            """)
    int countNormalUsersByEmail(@Param("email") String email);

    @Insert("""
            insert into users (
                login_id,
                password_hash,
                email,
                email_verified_yn,
                account_status,
                email_substitute_yn,
                verification_origin,
                system_use_yn,
                status,
                change_reason
            ) values (
                #{user.loginId},
                #{user.passwordHash},
                #{user.email},
                'N',
                'PENDING_EMAIL',
                'N',
                null,
                'Y',
                'ACTIVE',
                '회원가입 이메일 인증 대기'
            )
            """)
    int insertPendingSignupUser(@Param("user") NewSignupUser user);

    @Select("select user_id from users where login_id = #{loginId}")
    Long findUserIdByLoginId(@Param("loginId") String loginId);

    @Select("""
            select user_id as "userId",
                   login_id as "loginId",
                   email as "email",
                   account_status as "accountStatus",
                   email_verified_yn as "emailVerifiedYn",
                   verification_origin as "verificationOrigin",
                   email_substitute_yn as "emailSubstituteYn",
                   status as "status"
            from users
            where email is not null
              and lower(email) = #{email}
            order by user_id asc
            limit 1
            """)
    ResendTargetUser findResendTargetByEmail(@Param("email") String email);

    @Insert("""
            insert into user_roles (user_id, role_code, assignment_type, approver_user_id, status, change_reason)
            values (#{userId}, 'R01', 'MANUAL', null, 'ACTIVE', '회원가입 기본 일반 사용자 역할')
            """)
    int assignDefaultGeneralUserRole(@Param("userId") Long userId);

    @Insert("""
            insert into user_roles (user_id, role_code, assignment_type, approver_user_id, status, change_reason)
            values (#{userId}, #{roleCode}, 'MANUAL', null, 'ACTIVE', '회원가입 역할 부여')
            """)
    int assignRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    record NewSignupUser(String loginId, String passwordHash, String email) {
    }

    record ResendTargetUser(
            Long userId,
            String loginId,
            String email,
            String accountStatus,
            String emailVerifiedYn,
            String verificationOrigin,
            String emailSubstituteYn,
            String status
    ) {
    }
}
