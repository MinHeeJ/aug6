package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Reads the existing authorization, period, and finalization facts used by education-achievement write guards.
 */
@Mapper
public interface EducationAchievementAccessMapper {
    /**
     * Checks whether the requested organization is in the current actor's organization mapping.
     */
    @Select("""
            SELECT COUNT(*)
            FROM organization_user_mappings
            WHERE user_id = #{userId}
              AND organization_code = #{organizationCode}
              AND mapping_type = 'ORGANIZATION'
              AND status = 'ACTIVE'
            """)
    int countAuthorizedOrganization(
            @Param("userId") Long userId,
            @Param("organizationCode") String organizationCode
    );

    /**
     * Checks the active education input period at the server-side processing time.
     */
    @Select("""
            SELECT COUNT(*)
            FROM input_period_settings
            WHERE evaluation_year = #{evaluationYear}
              AND area_code = 'EDUCATION'
              AND organization_code = #{organizationCode}
              AND active_yn = 'Y'
              AND start_at <= #{processedAt}
              AND end_at >= #{processedAt}
            """)
    int countActiveInputPeriod(
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("processedAt") LocalDateTime processedAt
    );

    /**
     * Finds a non-cancelled finalization that makes the target user's achievement row immutable.
     */
    @Select("""
            SELECT COUNT(*)
            FROM evaluation_finalizations
            WHERE target_user_id = #{targetUserId}
              AND evaluation_year = #{evaluationYear}
              AND final_status = 'EVALUATION_CONFIRMED'
            """)
    int countEvaluationFinalizationLock(
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear
    );

    /**
     * Checks whether an achievement occurrence date is inside the configured education evaluation period.
     */
    @Select("""
            SELECT COUNT(*)
            FROM evaluation_date_settings
            WHERE evaluation_year = #{evaluationYear}
              AND area_code = 'EDUCATION'
              AND organization_code = #{organizationCode}
              AND active_yn = 'Y'
              AND #{occurredDate} BETWEEN start_at::date AND end_at::date
            """)
    int countOccurredDateInEvaluationPeriod(
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("occurredDate") LocalDate occurredDate
    );
}
