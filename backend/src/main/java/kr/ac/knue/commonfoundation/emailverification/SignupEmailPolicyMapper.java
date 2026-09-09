package kr.ac.knue.commonfoundation.emailverification;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SignupEmailPolicyMapper {
    @Select("""
            select count(*)
            from users
            where email is not null
              and lower(email) = #{normalizedEmail}
              and coalesce(email_substitute_yn, 'N') = 'N'
              and status != 'DELETED'
            """)
    int countNormalUsersByEmail(@Param("normalizedEmail") String normalizedEmail);
}
