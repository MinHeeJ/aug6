package kr.ac.knue.commonfoundation.emailverification;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmailVerificationEventMapper {
    @Insert("""
            insert into email_verification_events (user_id, event_type, token_id, normalized_email, actor_type, event_reason)
            values (#{userId}, #{eventType}, #{tokenId}, #{normalizedEmail}, #{actorType}, #{eventReason})
            """)
    int insertEvent(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("tokenId") Long tokenId,
            @Param("normalizedEmail") String normalizedEmail,
            @Param("actorType") String actorType,
            @Param("eventReason") String eventReason
    );

    @Select("""
            select event_id as "eventId",
                   user_id as "userId",
                   event_type as "eventType",
                   occurred_at as "occurredAt",
                   token_id as "tokenId",
                   normalized_email as "normalizedEmail",
                   actor_type as "actorType",
                   event_reason as "eventReason"
            from email_verification_events
            where user_id = #{userId}
            order by occurred_at desc, event_id desc
            """)
    List<EmailVerificationEvent> findByUserId(@Param("userId") Long userId);

    @Select("""
            select count(*)
            from email_verification_events
            where user_id = #{userId}
              and event_type = #{eventType}
            """)
    int countByUserAndType(@Param("userId") Long userId, @Param("eventType") String eventType);
}
