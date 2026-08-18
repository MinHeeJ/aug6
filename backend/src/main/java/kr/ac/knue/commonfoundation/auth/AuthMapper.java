package kr.ac.knue.commonfoundation.auth;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthMapper {
    @Select("""
            select u.user_id as "userId", u.login_id as "loginId", u.password_hash as "passwordHash",
                   u.employee_no as "employeeNo", coalesce(k.name, u.login_id) as "name"
            from users u
            left join korus_personnel_snapshots k on k.employee_no = u.employee_no
            where u.login_id = #{loginId} and u.system_use_yn = 'Y' and u.status = 'ACTIVE'
            """)
    AccountRow findAccountByLoginId(@Param("loginId") String loginId);

    @Select("""
            select role_code from user_roles
            where user_id = #{userId} and status = 'ACTIVE'
              and valid_start_date <= CURRENT_DATE
              and (valid_end_date is null or valid_end_date >= CURRENT_DATE)
            order by role_code
            """)
    List<String> findActiveRoleCodes(@Param("userId") Long userId);

    @Insert("""
            insert into sessions (session_id, user_id, expires_at, status)
            values (#{sessionId}, #{userId}, #{expiresAt}, 'ACTIVE')
            """)
    void insertSession(@Param("sessionId") String sessionId, @Param("userId") Long userId, @Param("expiresAt") LocalDateTime expiresAt);

    @Select("""
            select s.session_id as "sessionId", u.user_id as "userId", u.login_id as "loginId",
                   u.employee_no as "employeeNo", coalesce(k.name, u.login_id) as "name"
            from sessions s
            join users u on u.user_id = s.user_id
            left join korus_personnel_snapshots k on k.employee_no = u.employee_no
            where s.session_id = #{sessionId} and s.status = 'ACTIVE' and s.expires_at > CURRENT_TIMESTAMP
              and u.system_use_yn = 'Y' and u.status = 'ACTIVE'
            """)
    SessionUserRow findUserByActiveSession(@Param("sessionId") String sessionId);

    @Update("update sessions set last_accessed_at = CURRENT_TIMESTAMP where session_id = #{sessionId} and status = 'ACTIVE'")
    void touchSession(@Param("sessionId") String sessionId);

    @Update("update sessions set status = 'LOGGED_OUT', last_accessed_at = CURRENT_TIMESTAMP where session_id = #{sessionId} and status = 'ACTIVE'")
    void logout(@Param("sessionId") String sessionId);

    record AccountRow(Long userId, String loginId, String passwordHash, String employeeNo, String name) {}
    record SessionUserRow(String sessionId, Long userId, String loginId, String employeeNo, String name) {}
}
