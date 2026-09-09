package kr.ac.knue.commonfoundation.emailverification;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MailDeliveryAttemptMapper {
    @Insert("""
            insert into mail_delivery_attempts (user_id, normalized_email, requester_ip, delivery_status, diagnostic_category, diagnostic_message, retry_eligible_yn, requested_at)
            values (#{userId}, #{normalizedEmail}, #{requesterIp}, 'REQUESTED', 'NONE', null, 'N', CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "attemptId", keyColumn = "attempt_id")
    int insertRequested(NewMailDeliveryAttempt attempt);

    @Select("""
            select attempt_id as "attemptId",
                   user_id as "userId",
                   normalized_email as "normalizedEmail",
                   delivery_status as "deliveryStatus",
                   diagnostic_category as "diagnosticCategory",
                   diagnostic_message as "diagnosticMessage",
                   retry_eligible_yn as "retryEligibleYn",
                   requested_at as "requestedAt",
                   completed_at as "completedAt",
                   created_at as "createdAt",
                   updated_at as "updatedAt"
            from mail_delivery_attempts
            where attempt_id = #{attemptId}
            """)
    MailDeliveryAttempt findById(@Param("attemptId") Long attemptId);

    @Update("""
            update mail_delivery_attempts
            set delivery_status = 'SENT',
                diagnostic_category = 'NONE',
                diagnostic_message = null,
                retry_eligible_yn = 'N',
                completed_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            where attempt_id = #{attemptId}
            """)
    int markSent(@Param("attemptId") Long attemptId);

    @Update("""
            update mail_delivery_attempts
            set delivery_status = #{deliveryStatus},
                diagnostic_category = #{diagnosticCategory},
                diagnostic_message = #{diagnosticMessage},
                retry_eligible_yn = #{retryEligibleYn},
                completed_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            where attempt_id = #{attemptId}
            """)
    int markFailure(
            @Param("attemptId") Long attemptId,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("diagnosticCategory") String diagnosticCategory,
            @Param("diagnosticMessage") String diagnosticMessage,
            @Param("retryEligibleYn") String retryEligibleYn
    );

    @Select("""
            select attempt_id as "attemptId",
                   user_id as "userId",
                   normalized_email as "normalizedEmail",
                   delivery_status as "deliveryStatus",
                   diagnostic_category as "diagnosticCategory",
                   diagnostic_message as "diagnosticMessage",
                   retry_eligible_yn as "retryEligibleYn",
                   requested_at as "requestedAt",
                   completed_at as "completedAt",
                   created_at as "createdAt",
                   updated_at as "updatedAt"
            from mail_delivery_attempts
            where normalized_email = #{normalizedEmail}
            order by requested_at desc, attempt_id desc
            limit 1
            """)
    MailDeliveryAttempt findMostRecentByEmail(@Param("normalizedEmail") String normalizedEmail);

    @Select("""
            select count(*)
            from mail_delivery_attempts
            where normalized_email = #{normalizedEmail}
              and requested_at > CURRENT_TIMESTAMP - interval '60 seconds'
            """)
    int countRecentAttemptsForEmailThrottle(@Param("normalizedEmail") String normalizedEmail);

    @Select("""
            select count(*)
            from mail_delivery_attempts
            where normalized_email = #{normalizedEmail}
              and user_id = #{userId}
              and requested_at > CURRENT_TIMESTAMP - (#{seconds} * interval '1 second')
            """)
    int countRecentAttemptsForThrottle(
            @Param("userId") Long userId,
            @Param("normalizedEmail") String normalizedEmail,
            @Param("seconds") int seconds
    );

    @Select("""
            <script>
            select count(*)
            from mail_delivery_attempts
            where requested_at > CURRENT_TIMESTAMP - (#{seconds} * interval '1 second')
            <if test="normalizedEmail != null">
              and normalized_email = #{normalizedEmail}
            </if>
            <if test="requesterIp != null">
              and requester_ip = #{requesterIp}
            </if>
            </script>
            """)
    int countAttemptsSince(
            @Param("normalizedEmail") String normalizedEmail,
            @Param("requesterIp") String requesterIp,
            @Param("seconds") int seconds
    );

    class NewMailDeliveryAttempt {
        private Long attemptId;
        private final Long userId;
        private final String normalizedEmail;
        private final String requesterIp;

        public NewMailDeliveryAttempt(Long userId, String normalizedEmail, String requesterIp) {
            this.userId = userId;
            this.normalizedEmail = normalizedEmail;
            this.requesterIp = requesterIp;
        }

        public Long getAttemptId() {
            return attemptId;
        }

        public void setAttemptId(Long attemptId) {
            this.attemptId = attemptId;
        }

        public Long getUserId() {
            return userId;
        }

        public String getNormalizedEmail() {
            return normalizedEmail;
        }

        public String getRequesterIp() {
            return requesterIp;
        }
    }
}
