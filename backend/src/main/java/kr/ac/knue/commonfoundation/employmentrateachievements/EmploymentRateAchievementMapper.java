package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for the EMPLOYMENT_RATE common-header records
 * and R07 batch-job read models.
 */
@Mapper
public interface EmploymentRateAchievementMapper {
    @Select("""
            <script>
            SELECT
                ea.achievement_id AS achievementId,
                ea.management_no AS managementNo,
                ea.teacher_user_id AS teacherUserId,
                u.login_id AS teacherName,
                ea.evaluation_year AS evaluationYear,
                ea.management_item_code AS managementItemCode,
                ea.achievement_date AS achievementDate,
                ea.achievement_name AS achievementName,
                ea.attachment_ids::text AS attachmentIds,
                ea.achievement_status AS achievementStatus,
                ea.created_at AS createdAt,
                ea.updated_at AS updatedAt
            FROM education_achievements ea
            JOIN users u ON u.user_id = ea.teacher_user_id
            WHERE ea.achievement_type = 'EMPLOYMENT_RATE'
              AND ea.deleted_yn = 'N'
            <choose>
              <when test="roles.contains('R01')">
                AND ea.teacher_user_id = #{requesterUserId}
              </when>
              <when test="roles.contains('R02')">
                AND EXISTS (
                  SELECT 1
                  FROM organization_user_mappings requester_mapping
                  JOIN organization_user_mappings target_mapping
                    ON target_mapping.organization_code = requester_mapping.organization_code
                  WHERE requester_mapping.user_id = #{requesterUserId}
                    AND requester_mapping.status = 'ACTIVE'
                    AND target_mapping.user_id = ea.teacher_user_id
                    AND target_mapping.status = 'ACTIVE'
                )
              </when>
              <when test="roles.contains('R04')">
                AND EXISTS (
                  SELECT 1
                  FROM evaluation_organization_mappings permission_mapping
                  JOIN organization_user_mappings target_mapping
                    ON target_mapping.organization_code = permission_mapping.organization_code
                  WHERE permission_mapping.user_id = #{requesterUserId}
                    AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                    AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                    AND target_mapping.user_id = ea.teacher_user_id
                    AND target_mapping.status = 'ACTIVE'
                )
              </when>
              <when test="roles.contains('R07')">
                AND ea.teacher_user_id = #{requesterUserId}
              </when>
              <otherwise>
                AND 1 = 0
              </otherwise>
            </choose>
            <if test="criteria.normalizedManagementNo() != null">
              AND ea.management_no = #{criteria.normalizedManagementNo}
            </if>
            <if test="criteria.normalizedManagementItemCode() != null">
              AND ea.management_item_code = #{criteria.normalizedManagementItemCode}
            </if>
            <if test="criteria.normalizedAchievementStatus() != null">
              AND ea.achievement_status = #{criteria.normalizedAchievementStatus}
            </if>
            ORDER BY ea.achievement_date DESC, ea.achievement_id DESC
            LIMIT #{criteria.safePageSize}
            OFFSET #{criteria.offset}
            </script>
            """)
    List<EmploymentRateAchievementRow> list(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM education_achievements ea
            WHERE ea.achievement_type = 'EMPLOYMENT_RATE'
              AND ea.deleted_yn = 'N'
            <choose>
              <when test="roles.contains('R01')">
                AND ea.teacher_user_id = #{requesterUserId}
              </when>
              <when test="roles.contains('R02')">
                AND EXISTS (
                  SELECT 1
                  FROM organization_user_mappings requester_mapping
                  JOIN organization_user_mappings target_mapping
                    ON target_mapping.organization_code = requester_mapping.organization_code
                  WHERE requester_mapping.user_id = #{requesterUserId}
                    AND requester_mapping.status = 'ACTIVE'
                    AND target_mapping.user_id = ea.teacher_user_id
                    AND target_mapping.status = 'ACTIVE'
                )
              </when>
              <when test="roles.contains('R04')">
                AND EXISTS (
                  SELECT 1
                  FROM evaluation_organization_mappings permission_mapping
                  JOIN organization_user_mappings target_mapping
                    ON target_mapping.organization_code = permission_mapping.organization_code
                  WHERE permission_mapping.user_id = #{requesterUserId}
                    AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                    AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                    AND target_mapping.user_id = ea.teacher_user_id
                    AND target_mapping.status = 'ACTIVE'
                )
              </when>
              <when test="roles.contains('R07')">
                AND ea.teacher_user_id = #{requesterUserId}
              </when>
              <otherwise>
                AND 1 = 0
              </otherwise>
            </choose>
            <if test="criteria.normalizedManagementNo() != null">
              AND ea.management_no = #{criteria.normalizedManagementNo}
            </if>
            <if test="criteria.normalizedManagementItemCode() != null">
              AND ea.management_item_code = #{criteria.normalizedManagementItemCode}
            </if>
            <if test="criteria.normalizedAchievementStatus() != null">
              AND ea.achievement_status = #{criteria.normalizedAchievementStatus}
            </if>
            </script>
            """)
    long count(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            SELECT
                ea.achievement_id AS achievementId,
                ea.management_no AS managementNo,
                ea.teacher_user_id AS teacherUserId,
                u.login_id AS teacherName,
                ea.evaluation_year AS evaluationYear,
                ea.management_item_code AS managementItemCode,
                ea.achievement_date AS achievementDate,
                ea.achievement_name AS achievementName,
                ea.attachment_ids::text AS attachmentIds,
                ea.achievement_status AS achievementStatus,
                ea.created_at AS createdAt,
                ea.updated_at AS updatedAt
            FROM education_achievements ea
            JOIN users u ON u.user_id = ea.teacher_user_id
            WHERE ea.achievement_id = #{achievementId}
              AND ea.achievement_type = 'EMPLOYMENT_RATE'
              AND ea.deleted_yn = 'N'
            """)
    EmploymentRateAchievementRow find(@Param("achievementId") Long achievementId);

    @Select("""
            <script>
            SELECT EXISTS (
                SELECT 1
                FROM education_achievements ea
                WHERE ea.achievement_id = #{achievementId}
                  AND ea.achievement_type = 'EMPLOYMENT_RATE'
                  AND ea.deleted_yn = 'N'
                <choose>
                  <when test="roles.contains('R01')">
                    AND ea.teacher_user_id = #{requesterUserId}
                  </when>
                  <when test="roles.contains('R02')">
                    AND EXISTS (
                      SELECT 1
                      FROM organization_user_mappings requester_mapping
                      JOIN organization_user_mappings target_mapping
                        ON target_mapping.organization_code = requester_mapping.organization_code
                      WHERE requester_mapping.user_id = #{requesterUserId}
                        AND requester_mapping.status = 'ACTIVE'
                        AND target_mapping.user_id = ea.teacher_user_id
                        AND target_mapping.status = 'ACTIVE'
                    )
                  </when>
                  <when test="roles.contains('R04')">
                    AND EXISTS (
                      SELECT 1
                      FROM evaluation_organization_mappings permission_mapping
                      JOIN organization_user_mappings target_mapping
                        ON target_mapping.organization_code = permission_mapping.organization_code
                      WHERE permission_mapping.user_id = #{requesterUserId}
                        AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                        AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                        AND target_mapping.user_id = ea.teacher_user_id
                        AND target_mapping.status = 'ACTIVE'
                    )
                  </when>
                  <when test="roles.contains('R07')">
                    AND ea.teacher_user_id = #{requesterUserId}
                  </when>
                  <otherwise>
                    AND 1 = 0
                  </otherwise>
                </choose>
            )
            </script>
            """)
    boolean isVisible(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            INSERT INTO education_achievements (
                management_no, achievement_type, teacher_user_id, organization_code,
                evaluation_year, management_item_code, achievement_date, achievement_name,
                attachment_ids, achievement_status, deleted_yn, created_by, updated_by
            )
            SELECT
                #{managementNo}, 'EMPLOYMENT_RATE', #{userId}, mapping.organization_code,
                #{evaluationYear}, #{managementItemCode}, #{achievementDate}, #{achievementName},
                CAST(#{attachmentIds} AS jsonb), 'DRAFT', 'N', #{userId}, #{userId}
            FROM organization_user_mappings mapping
            WHERE mapping.user_id = #{userId}
              AND mapping.status = 'ACTIVE'
            ORDER BY mapping.mapping_id
            LIMIT 1
            RETURNING achievement_id
            """)
    Long insert(
            @Param("managementNo") String managementNo,
            @Param("userId") Long userId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                achievement_date = #{achievementDate},
                achievement_name = #{achievementName},
                attachment_ids = CAST(#{attachmentIds} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{userId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'EMPLOYMENT_RATE'
              AND deleted_yn = 'N'
            """)
    int update(
            @Param("achievementId") Long achievementId,
            @Param("userId") Long userId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds);

    @Insert("""
            INSERT INTO data_change_histories (
                target_business, target_key, change_type, field_name, before_value,
                after_value, changed_by, change_reason
            ) VALUES (
                'education_achievements', #{achievementId}::text, #{changeType},
                'employment_rate_achievement', #{beforeValue}, #{afterValue}, #{userId}, #{reason}
            )
            """)
    void insertChangeHistory(
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("userId") Long userId,
            @Param("reason") String reason);

    @Select("""
            SELECT
                batch_job_id AS batchJobId,
                evaluation_year AS evaluationYear,
                target_condition_json::text AS targetCondition,
                action_type AS actionType,
                job_status AS jobStatus,
                total_count AS totalCount,
                success_count AS successCount,
                failure_count AS failureCount,
                excluded_count AS excludedCount,
                requested_at AS requestedAt
            FROM employment_rate_batch_jobs
            WHERE batch_job_id = #{jobId}
            """)
    EmploymentRateBulkJobRow findBatchJob(@Param("jobId") String jobId);

    @Delete("DELETE FROM employment_rate_batch_jobs WHERE batch_job_id = #{jobId}")
    int deleteBatchJob(@Param("jobId") String jobId);
}
