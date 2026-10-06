package kr.ac.knue.commonfoundation.signupimplementation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Signup-specific MyBatis adapter; no credential-bearing request object is sent to SQL. */
@Mapper
public interface SignupMapper {
    boolean loginIdExists(@Param("userId") String userId);

    boolean emailExists(@Param("email") String email);

    /** INSERT RETURNING supplies the parent key before the R01 detail is inserted. */
    Long insertAccount(
            @Param("userId") String userId,
            @Param("passwordHash") String passwordHash,
            @Param("email") String email
    );

    int insertDefaultRole(@Param("internalUserId") Long internalUserId);
}
