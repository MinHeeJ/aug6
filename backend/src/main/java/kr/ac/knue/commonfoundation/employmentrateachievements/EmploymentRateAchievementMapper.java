package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Persists employment-rate records in the BASIC-83 shared header table and
 * applies the existing R01/R02/R04 data-scope model in every read query.
 */
@Mapper
public interface EmploymentRateAchievementMapper {
    @Select("""
            SELECT u.user_id
            FROM users u
            WHERE u.employee_no = #{employeeNo}
                AND u.status = 'ACTIVE'
                AND u.system_use_yn = 'Y'
            """)
    Long findActiveUserIdByEmployeeNo(@Param("employeeNo") String employeeNo);

    @Select("""
            SELECT
                a.achievement_id AS "achievementId",
                a.management_no AS "managementNo",
                a.teacher_user_id AS "teacherUserId",
                u.login_id AS "teacherName",
                a.evaluation_year AS "evaluationYear",
                a.management_item_code AS "managementItemCode",
                a.achievement_date AS "achievementDate",
                a.achievement_name AS "achievementName",
                a.attachment_ref AS "attachmentRef",
                a.achievement_status AS "achievementStatus",
                a.created_at AS "createdAt",
                a.updated_at AS "updatedAt"
            FROM education_achievements a
            JOIN users u
                ON u.user_id = a.teacher_user_id
            WHERE a.achievement_type = 'EMPLOYMENT_RATE'
                AND a.deleted_yn = 'N'
                AND (
                    #{allScope}
                    OR (#{ownScope} AND a.teacher_user_id = #{requesterUserId})
                    OR (
                        #{organizationScope}
                        AND EXISTS (
                            SELECT 1
                            FROM organization_user_mappings requester_mapping
                            JOIN organization_user_mappings target_mapping
                                ON target_mapping.organization_code = requester_mapping.organization_code
                            WHERE requester_mapping.user_id = #{requesterUserId}
                                AND requester_mapping.status = 'ACTIVE'
                                AND target_mapping.user_id = a.teacher_user_id
                                AND target_mapping.status = 'ACTIVE'
                        )
                    )
                    OR (
                        #{certificationScope}
                        AND EXISTS (
                            SELECT 1
                            FROM evaluation_organization_mappings permission_mapping
                            JOIN organization_user_mappings target_mapping
                                ON target_mapping.organization_code = permission_mapping.organization_code
                            WHERE permission_mapping.user_id = #{requesterUserId}
                                AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                                AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                                AND target_mapping.user_id = a.teacher_user_id
                                AND target_mapping.status = 'ACTIVE'
                        )
                    )
                )
            ORDER BY a.achievement_date DESC, a.achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            """)
    java.util.List<Map<String, Object>> list(
            @Param("pageSize") int pageSize,
            @Param("offset") int offset,
            @Param("requesterUserId") Long requesterUserId,
            @Param("ownScope") boolean ownScope,
            @Param("organizationScope") boolean organizationScope,
            @Param("certificationScope") boolean certificationScope,
            @Param("allScope") boolean allScope);

    @Select("""
            SELECT COUNT(*)
            FROM education_achievements a
            WHERE a.achievement_type = 'EMPLOYMENT_RATE'
                AND a.deleted_yn = 'N'
                AND (
                    #{allScope}
                    OR (#{ownScope} AND a.teacher_user_id = #{requesterUserId})
                    OR (
                        #{organizationScope}
                        AND EXISTS (
                            SELECT 1
                            FROM organization_user_mappings requester_mapping
                            JOIN organization_user_mappings target_mapping
                                ON target_mapping.organization_code = requester_mapping.organization_code
                            WHERE requester_mapping.user_id = #{requesterUserId}
                                AND requester_mapping.status = 'ACTIVE'
                                AND target_mapping.user_id = a.teacher_user_id
                                AND target_mapping.status = 'ACTIVE'
                        )
                    )
                    OR (
                        #{certificationScope}
                        AND EXISTS (
                            SELECT 1
                            FROM evaluation_organization_mappings permission_mapping
                            JOIN organization_user_mappings target_mapping
                                ON target_mapping.organization_code = permission_mapping.organization_code
                            WHERE permission_mapping.user_id = #{requesterUserId}
                                AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                                AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                                AND target_mapping.user_id = a.teacher_user_id
                                AND target_mapping.status = 'ACTIVE'
                        )
                    )
                )
            """)
    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("ownScope") boolean ownScope,
            @Param("organizationScope") boolean organizationScope,
            @Param("certificationScope") boolean certificationScope,
            @Param("allScope") boolean allScope);

    @Select("""
            SELECT
                a.achievement_id AS "achievementId",
                a.management_no AS "managementNo",
                a.teacher_user_id AS "teacherUserId",
                u.login_id AS "teacherName",
                a.evaluation_year AS "evaluationYear",
                a.management_item_code AS "managementItemCode",
                a.achievement_date AS "achievementDate",
                a.achievement_name AS "achievementName",
                a.attachment_ref AS "attachmentRef",
                a.achievement_status AS "achievementStatus",
                a.created_at AS "createdAt",
                a.updated_at AS "updatedAt"
            FROM education_achievements a
            JOIN users u
                ON u.user_id = a.teacher_user_id
            WHERE a.achievement_id = #{achievementId}
                AND a.achievement_type = 'EMPLOYMENT_RATE'
                AND a.deleted_yn = 'N'
            """)
    Map<String, Object> find(@Param("achievementId") Long achievementId);

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
    int countSharedOrganization(
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
            FROM education_achievements a
            WHERE a.achievement_type = 'EMPLOYMENT_RATE'
                AND a.deleted_yn = 'N'
                AND a.management_item_code = #{managementItemCode}
                AND a.achievement_date = #{achievementDate}
                AND COALESCE(a.achievement_name, '') = COALESCE(#{achievementName}, '')
            """)
    int countDuplicate(
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName);

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
                attachment_ref,
                achievement_status,
                deleted_yn,
                created_by,
                updated_by
            )
            SELECT
                #{managementNo},
                'EMPLOYMENT_RATE',
                #{userId},
                COALESCE(
                    (
                        SELECT mapping.organization_code
                        FROM organization_user_mappings mapping
                        WHERE mapping.user_id = #{userId}
                            AND mapping.status = 'ACTIVE'
                        ORDER BY mapping.created_at
                        LIMIT 1
                    ),
                    'KNUE'
                ),
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{achievementName},
                #{attachmentRef},
                'DRAFT',
                'N',
                #{userId},
                #{userId}
            """)
    void insert(
            @Param("managementNo") String managementNo,
            @Param("userId") Long userId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRef") String attachmentRef);

    @Select("""
            SELECT a.achievement_id
            FROM education_achievements a
            WHERE a.management_no = #{managementNo}
            """)
    Long findIdByManagementNo(@Param("managementNo") String managementNo);

    @Update("""
            UPDATE education_achievements a
            SET management_item_code = #{managementItemCode},
                achievement_date = #{achievementDate},
                achievement_name = #{achievementName},
                attachment_ref = #{attachmentRef},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{userId}
            WHERE a.achievement_id = #{achievementId}
                AND a.achievement_type = 'EMPLOYMENT_RATE'
                AND a.deleted_yn = 'N'
            """)
    int update(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRef") String attachmentRef,
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
                #{targetKey},
                #{changeType},
                'employmentRateAchievement',
                #{beforeValue},
                #{afterValue},
                #{userId},
                #{reason},
                #{requestId}
            )
            """)
    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("userId") Long userId,
            @Param("reason") String reason,
            @Param("requestId") String requestId);

    @Insert("""
            INSERT INTO excel_upload_files (
                upload_id, business_type, file_token, original_file_name, validation_status, uploader_user_id
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
    void insertUpload(
            @Param("uploadId") String uploadId,
            @Param("fileToken") String fileToken,
            @Param("originalFileName") String originalFileName,
            @Param("validationStatus") String validationStatus,
            @Param("userId") Long userId);

    @Insert("""
            INSERT INTO excel_upload_histories (
                upload_id, total_count, success_count, error_count, excluded_count, saved_count, processing_time_millis,
                processor_user_id
            )
            VALUES (#{uploadId}, #{totalCount}, #{successCount}, #{errorCount}, 0, #{savedCount}, 0, #{userId})
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
                error_id, upload_id, row_number, column_name, input_value, error_code, error_reason, correction_guide
            )
            VALUES (
                #{errorId}, #{uploadId}, #{rowNumber}, #{columnName}, #{inputValue}, #{errorCode}, #{errorReason},
                '현행 양식과 입력값을 확인하세요.'
            )
            """)
    void insertUploadError(
            @Param("errorId") String errorId,
            @Param("uploadId") String uploadId,
            @Param("rowNumber") int rowNumber,
            @Param("columnName") String columnName,
            @Param("inputValue") String inputValue,
            @Param("errorCode") String errorCode,
            @Param("errorReason") String errorReason);

    @Select("""
            SELECT
                j.batch_job_id AS "jobId",
                j.evaluation_year AS "evaluationYear",
                j.action_type AS "actionType",
                j.job_status AS "jobStatus",
                j.total_count AS "totalCount",
                j.processed_count AS "processedCount",
                j.success_count AS "successCount",
                j.failure_count AS "failureCount",
                j.requested_at AS "requestedAt",
                j.completed_at AS "completedAt"
            FROM employment_rate_batch_jobs j
            WHERE j.batch_job_id = #{jobId}
                AND j.requested_by = #{requesterUserId}
            """)
    Map<String, Object> findBulkJob(
            @Param("jobId") String jobId,
            @Param("requesterUserId") Long requesterUserId);
}
