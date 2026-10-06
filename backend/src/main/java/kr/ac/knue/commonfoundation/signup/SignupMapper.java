package kr.ac.knue.commonfoundation.signup;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Registration persistence against the existing users/user_roles tables. */
@Mapper
public interface SignupMapper {
    // Includes inactive/deleted accounts because login_id remains globally unique.
    boolean existsLoginId(@Param("loginId") String loginId);

    boolean existsEmail(@Param("email") String email);

    /** Returns the generated bigint directly from INSERT; a concurrent unique conflict returns null. */
    Long insertUser(
            @Param("loginId") String loginId,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash
    );

    int insertDefaultRole(@Param("userId") Long userId);
}
