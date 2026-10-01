package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Reads the existing organization, period, and finalization records required by
 * education-achievement mutation guards.
 */
@Mapper
public interface EducationAchievementGuardMapper {
    @Select("""
            SELECT COUNT(*)
            FROM organization_user_mappings requester_mapping
            JOIN organization_user_mappings target_mapping
                ON target_mapping.organization_code = requester_mapping.organization_code
            WHERE requester_mapping.user_id = #{requesterUserId}
              AND requester_mapping.status = 'ACTIVE'
              AND target_mapping.user_id = #{targetUserId}
              AND target_mapping.status = 'ACTIVE'
            """)
    int countSharedActiveOrganization(
            @Param("requesterUserId") Long requesterUserId,
            @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT COUNT(*)
            FROM evaluation_organization_mappings permission_mapping
            JOIN organization_user_mappings target_mapping
                ON target_mapping.organization_code = permission_mapping.organization_code
            WHERE permission_mapping.user_id = #{requesterUserId}
              AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
              AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
              AND target_mapping.user_id = #{targetUserId}
              AND target_mapping.status = 'ACTIVE'
            """)
    int countCertificationScope(
            @Param("requesterUserId") Long requesterUserId,
            @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT COUNT(*)
            FROM input_period_settings period
            WHERE period.evaluation_year = #{evaluationYear}
              AND period.area_code = 'EDUCATION'
              AND period.active_yn = 'Y'
              AND CURRENT_TIMESTAMP BETWEEN period.start_at AND period.end_at
              AND (
                    period.organization_code IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM organization_user_mappings target_mapping
                        WHERE target_mapping.user_id = #{targetUserId}
                          AND target_mapping.organization_code = period.organization_code
                          AND target_mapping.status = 'ACTIVE'
                    )
              )
            """)
    int countActiveInputPeriods(
            @Param("evaluationYear") String evaluationYear,
            @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT COUNT(*)
            FROM evaluation_finalizations finalization
            WHERE finalization.target_user_id = #{targetUserId}
              AND finalization.evaluation_year = #{evaluationYear}
              AND finalization.final_status = 'EVALUATION_CONFIRMED'
            """)
    int countEvaluationConfirmations(
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear);

    @Select("""
            SELECT COUNT(*)
            FROM evaluation_date_settings period
            WHERE period.evaluation_year = #{evaluationYear}
              AND period.area_code = 'EDUCATION'
              AND period.active_yn = 'Y'
              AND #{occurredDate} BETWEEN period.start_at::date AND period.end_at::date
              AND (
                    period.organization_code IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM organization_user_mappings target_mapping
                        WHERE target_mapping.user_id = #{targetUserId}
                          AND target_mapping.organization_code = period.organization_code
                          AND target_mapping.status = 'ACTIVE'
                    )
              )
            """)
    int countEvaluationDatePeriods(
            @Param("evaluationYear") String evaluationYear,
            @Param("targetUserId") Long targetUserId,
            @Param("occurredDate") LocalDate occurredDate);
}
