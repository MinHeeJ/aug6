package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Persistence adapter for the BASIC-83 employment-rate achievement ledger,
 * its upload audit records, and deferred batch-job results.
 */
@Mapper
public interface EmploymentRateAchievementMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.teacher_user_id AS "teacherUserId",
                owner.login_id AS "teacherLoginId",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.achievement_date AS "achievementDate",
                achievement.achievement_name AS "achievementName",
                achievement.attachment_ids::text AS "attachmentIdsJson",
                achievement.achievement_status AS "achievementStatus",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE'
                AND achievement.deleted_yn = 'N'
            <choose>
                <when test="roles.contains('R01')">
                    AND achievement.teacher_user_id = #{requesterUserId}
                </when>
                <when test="roles.contains('R02')">
                    AND EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                            AND requester_mapping.status = 'ACTIVE'
                            AND target_mapping.user_id = achievement.teacher_user_id
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
                            AND target_mapping.user_id = achievement.teacher_user_id
                            AND target_mapping.status = 'ACTIVE'
                    )
                </when>
                <when test="roles.contains('R07')">
                    <!-- R07 is authorized for operational download across the batch scope. -->
                </when>
                <otherwise>AND 1 = 0</otherwise>
            </choose>
            ORDER BY achievement.achievement_date DESC, achievement.achievement_id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<Map<String, Object>> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE'
                AND achievement.deleted_yn = 'N'
            <choose>
                <when test="roles.contains('R01')">
                    AND achievement.teacher_user_id = #{requesterUserId}
                </when>
                <when test="roles.contains('R02')">
                    AND EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                            AND requester_mapping.status = 'ACTIVE'
                            AND target_mapping.user_id = achievement.teacher_user_id
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
                            AND target_mapping.user_id = achievement.teacher_user_id
                            AND target_mapping.status = 'ACTIVE'
                    )
                </when>
                <when test="roles.contains('R07')">
                    <!-- R07 is authorized for operational download across the batch scope. -->
                </when>
                <otherwise>AND 1 = 0</otherwise>
            </choose>
            </script>
            """)
    long count(@Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);

    @Select("""
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.teacher_user_id AS "teacherUserId",
                owner.login_id AS "teacherLoginId",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.achievement_date AS "achievementDate",
                achievement.achievement_name AS "achievementName",
                achievement.attachment_ids::text AS "attachmentIdsJson",
                achievement.achievement_status AS "achievementStatus",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_id = #{achievementId}
                AND achievement.achievement_type = 'EMPLOYMENT_RATE'
                AND achievement.deleted_yn = 'N'
            """)
    Map<String, Object> find(@Param("achievementId") Long achievementId);

    @Select("""
            INSERT INTO education_achievements (
                achievement_type,
                teacher_user_id,
                organization_code,
                evaluation_year,
                management_item_code,
                achievement_date,
                achievement_name,
                attachment_ids,
                achievement_status,
                deleted_yn,
                created_by,
                updated_by
            )
            SELECT
                'EMPLOYMENT_RATE',
                #{userId},
                mapping.organization_code,
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{achievementName},
                CAST(#{attachmentIdsJson} AS jsonb),
                'DRAFT',
                'N',
                #{userId},
                #{userId}
            FROM organization_user_mappings mapping
            WHERE mapping.user_id = #{userId}
                AND mapping.status = 'ACTIVE'
            ORDER BY mapping.created_at
            LIMIT 1
            RETURNING achievement_id
            """)
    Long insertAchievement(EmploymentRateAchievementInsert command);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                evaluation_year = EXTRACT(YEAR FROM #{achievementDate})::varchar,
                achievement_date = #{achievementDate},
                achievement_name = #{achievementName},
                attachment_ids = CAST(#{attachmentIdsJson} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{updatedBy}
            WHERE achievement_id = #{achievementId}
                AND achievement_type = 'EMPLOYMENT_RATE'
                AND deleted_yn = 'N'
                AND achievement_status != 'EVALUATION_CONFIRMED'
            """)
    int updateAchievement(EmploymentRateAchievementUpdate command);

    @Insert("""
            INSERT INTO education_achievement_status_histories (
                achievement_type,
                achievement_id,
                previous_status,
                next_status,
                action_type,
                opinion,
                processed_by
            )
            VALUES ('EMPLOYMENT_RATE', #{achievementId}, NULL, 'DRAFT', 'CREATE', #{opinion}, #{userId})
            """)
    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("opinion") String opinion,
            @Param("userId") Long userId);

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
                #{reason},
                #{requestId}
            )
            """)
    void insertChangeHistory(
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("userId") Long userId,
            @Param("reason") String reason,
            @Param("requestId") String requestId);

    @Insert("""
            INSERT INTO excel_upload_files (
                upload_id,
                business_type,
                file_token,
                original_file_name,
                validation_status,
                uploader_user_id
            )
            VALUES (
                #{uploadId},
                'EMPLOYMENT_RATE_ACHIEVEMENT',
                #{fileToken},
                #{originalFileName},
                #{validationStatus},
                #{userId}
            )
            """)
    void insertUploadFile(
            @Param("uploadId") String uploadId,
            @Param("fileToken") String fileToken,
            @Param("originalFileName") String originalFileName,
            @Param("validationStatus") String validationStatus,
            @Param("userId") Long userId);

    @Insert("""
            INSERT INTO excel_upload_histories (
                upload_id,
                total_count,
                success_count,
                error_count,
                excluded_count,
                saved_count,
                processing_time_millis,
                processor_user_id
            )
            VALUES (
                #{uploadId},
                #{totalCount},
                #{successCount},
                #{errorCount},
                0,
                #{savedCount},
                0,
                #{userId}
            )
            """)
    void insertUploadHistory(
            @Param("uploadId") String uploadId,
            @Param("totalCount") int totalCount,
            @Param("successCount") int successCount,
            @Param("errorCount") int errorCount,
            @Param("savedCount") int savedCount,
            @Param("userId") Long userId);

    @Insert("""
            INSERT INTO excel_upload_errors (
                error_id,
                upload_id,
                row_number,
                column_name,
                input_value,
                error_code,
                error_reason,
                correction_guide
            )
            VALUES (
                #{errorId},
                #{uploadId},
                #{rowNumber},
                #{columnName},
                #{inputValue},
                #{errorCode},
                #{errorReason},
                #{correctionGuide}
            )
            """)
    void insertUploadError(
            @Param("errorId") String errorId,
            @Param("uploadId") String uploadId,
            @Param("rowNumber") int rowNumber,
            @Param("columnName") String columnName,
            @Param("inputValue") String inputValue,
            @Param("errorCode") String errorCode,
            @Param("errorReason") String errorReason,
            @Param("correctionGuide") String correctionGuide);

    @Insert("""
            INSERT INTO employment_rate_batch_jobs (
                batch_job_id,
                evaluation_year,
                target_condition_json,
                action_type,
                job_status,
                requested_by,
                request_id,
                created_by,
                updated_by
            )
            VALUES (
                #{batchJobId},
                #{evaluationYear},
                CAST(#{targetConditionJson} AS jsonb),
                #{actionType},
                'REJECTED',
                #{userId},
                #{requestId},
                #{userId},
                #{userId}
            )
            """)
    void insertRejectedBatchJob(
            @Param("batchJobId") String batchJobId,
            @Param("evaluationYear") String evaluationYear,
            @Param("targetConditionJson") String targetConditionJson,
            @Param("actionType") String actionType,
            @Param("userId") Long userId,
            @Param("requestId") String requestId);

    @Select("""
            SELECT
                job.batch_job_id AS "batchJobId",
                job.evaluation_year AS "evaluationYear",
                job.target_condition_json::text AS "targetConditionJson",
                job.action_type AS "actionType",
                job.job_status AS "jobStatus",
                job.total_count AS "totalCount",
                job.success_count AS "successCount",
                job.failure_count AS "failureCount",
                job.unprocessed_count AS "unprocessedCount",
                job.request_id AS "requestId"
            FROM employment_rate_batch_jobs job
            WHERE job.batch_job_id = #{batchJobId}
            """)
    Map<String, Object> findBatchJob(@Param("batchJobId") String batchJobId);
}

record EmploymentRateAchievementInsert(
        Long userId,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String attachmentIdsJson,
        Long achievementId) {
}

record EmploymentRateAchievementUpdate(
        Long achievementId,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        String attachmentIdsJson,
        Long updatedBy) {
}
