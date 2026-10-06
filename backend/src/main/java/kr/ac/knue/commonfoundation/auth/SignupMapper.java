package kr.ac.knue.commonfoundation.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis persistence boundary for public account registration and default role assignment.
 */
@Mapper
public interface SignupMapper {
    int countByLoginId(@Param("loginId") String loginId);

    int countByEmail(@Param("email") String email);

    Long insertUser(
            @Param("loginId") String loginId,
            @Param("passwordHash") String passwordHash,
            @Param("email") String email);

    int insertDefaultRole(@Param("userId") Long userId);
}
