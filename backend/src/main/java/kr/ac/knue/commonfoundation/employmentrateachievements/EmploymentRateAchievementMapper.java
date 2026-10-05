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
 * MyBatis persistence boundary for the BASIC-83 employment-rate source rows,
 * audit history, and bulk-job result tables established by the foundation migration.
 */
@Mapper
public interface EmploymentRateAchievementMapper {
    @Select("""
            SELECT
                achievement.achievement_id AS achievementId,
                achievement.management_no AS managementNo,
                achievement.teacher_user_id AS teacherUserId,
                owner.login_id AS teacherName,
                achievement.evaluation_year AS evaluationYear,
                achievement.management_item_code AS managementItemCode,
                achievement.achievement_date AS achievementDate,
                achievement.achievement_name AS achievementName,
                achievement.achievement_status AS achievementStatus,
                CAST(achievement.attachment_refs AS varchar) AS attachmentRefs,
                achievement.created_at AS createdAt,
                achievement.updated_at AS updatedAt
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
              AND achievement.teacher_user_id = #{userId}
            ORDER BY achievement.achievement_date DESC, achievement.achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            """)
    List<EmploymentRateAchievementRow> list(@Param("userId") Long userId,
                                            @Param("pageSize") int pageSize,
                                            @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
              AND achievement.teacher_user_id = #{userId}
            """)
    long count(@Param("userId") Long userId);

    @Select("""
            SELECT
                achievement.achievement_id AS achievementId,
                achievement.management_no AS managementNo,
                achievement.teacher_user_id AS teacherUserId,
                owner.login_id AS teacherName,
                achievement.evaluation_year AS evaluationYear,
                achievement.management_item_code AS managementItemCode,
                achievement.achievement_date AS achievementDate,
                achievement.achievement_name AS achievementName,
                achievement.achievement_status AS achievementStatus,
                CAST(achievement.attachment_refs AS varchar) AS attachmentRefs,
                achievement.created_at AS createdAt,
                achievement.updated_at AS updatedAt
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
            """)
    EmploymentRateAchievementRow find(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT
                achievement.achievement_id AS achievementId,
                achievement.management_no AS managementNo,
                achievement.teacher_user_id AS teacherUserId,
                owner.login_id AS teacherName,
                achievement.evaluation_year AS evaluationYear,
                achievement.management_item_code AS managementItemCode,
                achievement.achievement_date AS achievementDate,
                achievement.achievement_name AS achievementName,
                achievement.achievement_status AS achievementStatus,
                CAST(achievement.attachment_refs AS varchar) AS attachmentRefs,
                achievement.created_at AS createdAt,
                achievement.updated_at AS updatedAt
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.management_no = #{managementNo}
              AND achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
            """)
    EmploymentRateAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    @Insert("""
            INSERT INTO education_achievements (
                management_no,
                achievement_type,
                teacher_user_id,
                organization_code,
                evaluation_year,
                management_item_code,
                achievement_date,
                achievement_name,
                attachment_refs,
                created_by,
                updated_by
            )
            SELECT
                #{managementNo},
                'EMPLOYMENT_RATE',
                #{userId},
                mapping.organization_code,
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{achievementName},
                CAST(#{attachmentRefs} AS jsonb),
                #{userId},
                #{userId}
            FROM organization_user_mappings mapping
            WHERE mapping.user_id = #{userId}
              AND mapping.status = 'ACTIVE'
            ORDER BY mapping.mapping_id
            LIMIT 1
            """)
    int insert(@Param("managementNo") String managementNo,
               @Param("userId") Long userId,
               @Param("evaluationYear") String evaluationYear,
               @Param("managementItemCode") String managementItemCode,
               @Param("achievementDate") LocalDate achievementDate,
               @Param("achievementName") String achievementName,
               @Param("attachmentRefs") String attachmentRefs);

    /** Confirms that the target faculty member is actively mapped to an organization. */
    @Select("""
            SELECT COUNT(*)
            FROM organization_user_mappings mapping
            WHERE mapping.user_id = #{userId}
              AND mapping.status = 'ACTIVE'
            """)
    int countActiveOrganizationMappings(@Param("userId") Long userId);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                evaluation_year = #{evaluationYear},
                achievement_date = #{achievementDate},
                achievement_name = #{achievementName},
                attachment_refs = CAST(#{attachmentRefs} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{userId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'EMPLOYMENT_RATE'
              AND deleted_yn = 'N'
            """)
    void update(@Param("achievementId") Long achievementId,
                @Param("userId") Long userId,
                @Param("managementItemCode") String managementItemCode,
                @Param("evaluationYear") String evaluationYear,
                @Param("achievementDate") LocalDate achievementDate,
                @Param("achievementName") String achievementName,
                @Param("attachmentRefs") String attachmentRefs);

    @Insert("""
            INSERT INTO data_change_histories (
                target_business,
                target_key,
                change_type,
                field_name,
                before_value,
                after_value,
                changed_by,
                change_reason,
                request_id
            )
            VALUES (
                'education_achievements',
                #{achievementId},
                #{changeType},
                'employment_rate_achievement',
                #{beforeValue},
                #{afterValue},
                #{userId},
                #{changeReason},
                #{requestId}
            )
            """)
    void insertChangeHistory(@Param("achievementId") String achievementId,
                             @Param("changeType") String changeType,
                             @Param("beforeValue") String beforeValue,
                             @Param("afterValue") String afterValue,
                             @Param("userId") Long userId,
                             @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);

    @Select("""
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.management_no = #{managementNo}
              AND achievement.deleted_yn = 'N'
            """)
    int countManagementNo(@Param("managementNo") String managementNo);

    @Insert("""
            INSERT INTO employment_rate_batch_jobs (
                batch_job_id,
                evaluation_year,
                target_condition_json,
                action_type,
                job_status,
                total_count,
                unprocessed_count,
                request_id,
                requested_by,
                created_by,
                updated_by
            )
            VALUES (
                #{jobId},
                #{evaluationYear},
                CAST(#{targetConditionJson} AS jsonb),
                #{actionType},
                'REQUESTED',
                #{totalCount},
                #{totalCount},
                #{requestId},
                #{userId},
                #{userId},
                #{userId}
            )
            """)
    void insertBulkJob(@Param("jobId") String jobId,
                       @Param("evaluationYear") String evaluationYear,
                       @Param("targetConditionJson") String targetConditionJson,
                       @Param("actionType") String actionType,
                       @Param("totalCount") int totalCount,
                       @Param("requestId") String requestId,
                       @Param("userId") Long userId);

    @Insert("""
            INSERT INTO employment_rate_batch_job_items (
                batch_job_id,
                target_user_id,
                processed_yn,
                unprocessed_reason,
                created_by,
                updated_by
            )
            VALUES (
                #{jobId},
                #{targetUserId},
                'N',
                '처리 대기',
                #{userId},
                #{userId}
            )
            """)
    void insertBulkJobItem(
            @Param("jobId") String jobId,
            @Param("targetUserId") Long targetUserId,
            @Param("userId") Long userId);

    @Select("""
            SELECT
                job.batch_job_id AS jobId,
                job.evaluation_year AS evaluationYear,
                job.action_type AS actionType,
                job.job_status AS jobStatus,
                job.total_count AS totalCount,
                job.processed_count AS processedCount,
                job.unprocessed_count AS unprocessedCount,
                job.requested_at AS requestedAt
            FROM employment_rate_batch_jobs job
            WHERE job.batch_job_id = #{jobId}
              AND job.requested_by = #{userId}
            """)
    EmploymentRateBulkJobRow findBulkJob(
            @Param("jobId") String jobId,
            @Param("userId") Long userId);

    @Select("""
            SELECT
                item.target_user_id AS targetUserId,
                item.achievement_id AS achievementId,
                item.processed_yn = 'Y' AS processed,
                item.unprocessed_reason AS unprocessedReason
            FROM employment_rate_batch_job_items item
            WHERE item.batch_job_id = #{jobId}
            ORDER BY item.batch_job_item_id
            """)
    List<EmploymentRateBulkJobItem> findBulkJobItems(@Param("jobId") String jobId);

    @Delete("""
            DELETE FROM employment_rate_batch_jobs
            WHERE batch_job_id = #{jobId}
            """)
    void deleteBulkJob(@Param("jobId") String jobId);
}
