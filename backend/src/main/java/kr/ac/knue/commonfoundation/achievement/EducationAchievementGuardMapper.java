package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Reads existing authorization, period, and finalization facts without owning a second policy
 * store. Feature services call these checks before writing any education-achievement table.
 */
@Mapper
public interface EducationAchievementGuardMapper {
    @Select("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM evaluation_organization_mappings mapping
                WHERE mapping.user_id = #{userId}
                  AND mapping.organization_code = #{organizationCode}
                  AND mapping.business_type = 'FACULTY_ACHIEVEMENT'
                  AND mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
            ) THEN 1 ELSE 0 END
            """)
    int existsAuthorizedEvaluationOrganization(
            @Param("userId") Long userId,
            @Param("organizationCode") String organizationCode
    );

    @Select("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM input_period_settings period
                WHERE period.evaluation_year = #{evaluationYear}
                  AND period.area_code = 'EDUCATION'
                  AND period.organization_code = #{organizationCode}
                  AND period.active_yn = 'Y'
                  AND period.start_at <= #{checkedAt}
                  AND period.end_at >= #{checkedAt}
            ) THEN 1 ELSE 0 END
            """)
    int existsOpenInputPeriod(
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("checkedAt") LocalDateTime checkedAt
    );

    @Select("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM evaluation_finalizations finalization
                WHERE finalization.target_user_id = #{targetUserId}
                  AND finalization.evaluation_year = #{evaluationYear}
                  AND finalization.final_status = 'EVALUATION_CONFIRMED'
            ) THEN 1 ELSE 0 END
            """)
    int existsEvaluationFinalization(
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear
    );

    @Select("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM evaluation_date_settings period
                WHERE period.evaluation_year = #{evaluationYear}
                  AND period.area_code = 'EDUCATION'
                  AND period.organization_code = #{organizationCode}
                  AND period.active_yn = 'Y'
                  AND period.start_at::date <= #{occurredDate}
                  AND period.end_at::date >= #{occurredDate}
            ) THEN 1 ELSE 0 END
            """)
    int existsOccurredDateWithinEvaluationPeriod(
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("occurredDate") LocalDate occurredDate
    );
}
