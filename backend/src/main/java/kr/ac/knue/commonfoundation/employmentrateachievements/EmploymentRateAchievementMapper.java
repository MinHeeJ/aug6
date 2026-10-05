package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for the BASIC-83 employment-rate header and
 * policy-gated bulk-job records.
 */
@Mapper
public interface EmploymentRateAchievementMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id,
                achievement.management_no,
                achievement.teacher_user_id,
                owner.login_id AS teacher_name,
                achievement.evaluation_year,
                achievement.management_item_code,
                achievement.achievement_date,
                achievement.achievement_name,
                achievement.attachment_ids::text AS attachment_ids,
                achievement.achievement_status,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
            <if test="managementItemCode != null and !managementItemCode.isBlank()">
              AND achievement.management_item_code = #{managementItemCode}
            </if>
            <if test="achievementStatus != null and !achievementStatus.isBlank()">
              AND achievement.achievement_status = #{achievementStatus}
            </if>
            <choose>
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
              <when test="roles.contains('R01')">
                AND achievement.teacher_user_id = #{requesterUserId}
              </when>
              <otherwise>
                AND 1 = 0
              </otherwise>
            </choose>
            ORDER BY achievement.achievement_date DESC, achievement.achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "achievement_id", javaType = Long.class, id = true),
        @Arg(column = "management_no", javaType = String.class),
        @Arg(column = "teacher_user_id", javaType = Long.class),
        @Arg(column = "teacher_name", javaType = String.class),
        @Arg(column = "evaluation_year", javaType = String.class),
        @Arg(column = "management_item_code", javaType = String.class),
        @Arg(column = "achievement_date", javaType = LocalDate.class),
        @Arg(column = "achievement_name", javaType = String.class),
        @Arg(column = "attachment_ids", javaType = String.class),
        @Arg(column = "achievement_status", javaType = String.class),
        @Arg(column = "created_at", javaType = java.time.LocalDateTime.class),
        @Arg(column = "updated_at", javaType = java.time.LocalDateTime.class)
    })
    List<EmploymentRateAchievementRow> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementStatus") String achievementStatus,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    /** Counts exactly the rows visible through the list authorization and filter predicates. */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
            <if test="managementItemCode != null and !managementItemCode.isBlank()">
              AND achievement.management_item_code = #{managementItemCode}
            </if>
            <if test="achievementStatus != null and !achievementStatus.isBlank()">
              AND achievement.achievement_status = #{achievementStatus}
            </if>
            <choose>
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
              <when test="roles.contains('R01')">
                AND achievement.teacher_user_id = #{requesterUserId}
              </when>
              <otherwise>
                AND 1 = 0
              </otherwise>
            </choose>
            </script>
            """)
    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementStatus") String achievementStatus);

    @Select("""
            <script>
            SELECT
                achievement.achievement_id,
                achievement.management_no,
                achievement.teacher_user_id,
                owner.login_id AS teacher_name,
                achievement.evaluation_year,
                achievement.management_item_code,
                achievement.achievement_date,
                achievement.achievement_name,
                achievement.attachment_ids::text AS attachment_ids,
                achievement.achievement_status,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
            <choose>
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
              <when test="roles.contains('R01')">
                AND achievement.teacher_user_id = #{requesterUserId}
              </when>
              <otherwise>
                AND 1 = 0
              </otherwise>
            </choose>
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "achievement_id", javaType = Long.class, id = true),
        @Arg(column = "management_no", javaType = String.class),
        @Arg(column = "teacher_user_id", javaType = Long.class),
        @Arg(column = "teacher_name", javaType = String.class),
        @Arg(column = "evaluation_year", javaType = String.class),
        @Arg(column = "management_item_code", javaType = String.class),
        @Arg(column = "achievement_date", javaType = LocalDate.class),
        @Arg(column = "achievement_name", javaType = String.class),
        @Arg(column = "attachment_ids", javaType = String.class),
        @Arg(column = "achievement_status", javaType = String.class),
        @Arg(column = "created_at", javaType = java.time.LocalDateTime.class),
        @Arg(column = "updated_at", javaType = java.time.LocalDateTime.class)
    })
    EmploymentRateAchievementRow findScoped(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Insert("""
            INSERT INTO education_achievements (
                management_no,
                achievement_type,
                teacher_user_id,
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
            VALUES (
                #{managementNo},
                'EMPLOYMENT_RATE',
                #{teacherUserId},
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{achievementName},
                CAST(#{attachmentIds} AS jsonb),
                'DRAFT',
                'N',
                #{actorUserId},
                #{actorUserId}
            )
            """)
    void insert(EmploymentRateAchievementCommand command);

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
            VALUES (
                'EMPLOYMENT_RATE',
                #{achievementId},
                NULL,
                'DRAFT',
                'CREATE',
                '취업률 실적 최초 입력',
                #{processedBy}
            )
            """)
    void insertInitialStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("processedBy") Long processedBy);

    @Insert("""
            INSERT INTO data_change_histories (
                target_business,
                target_key,
                change_type,
                field_name,
                before_value,
                after_value,
                changed_by,
                change_reason
            )
            VALUES (
                'education_achievements',
                #{targetKey},
                #{changeType},
                'employment_rate',
                #{beforeValue},
                #{afterValue},
                #{changedBy},
                #{changeReason}
            )
            """)
    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);

    @Select("""
            SELECT
                achievement.achievement_id,
                achievement.management_no,
                achievement.teacher_user_id,
                owner.login_id AS teacher_name,
                achievement.evaluation_year,
                achievement.management_item_code,
                achievement.achievement_date,
                achievement.achievement_name,
                achievement.attachment_ids::text AS attachment_ids,
                achievement.achievement_status,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.management_no = #{managementNo}
              AND achievement.achievement_type = 'EMPLOYMENT_RATE'
              AND achievement.deleted_yn = 'N'
            """)
    @ConstructorArgs({
        @Arg(column = "achievement_id", javaType = Long.class, id = true),
        @Arg(column = "management_no", javaType = String.class),
        @Arg(column = "teacher_user_id", javaType = Long.class),
        @Arg(column = "teacher_name", javaType = String.class),
        @Arg(column = "evaluation_year", javaType = String.class),
        @Arg(column = "management_item_code", javaType = String.class),
        @Arg(column = "achievement_date", javaType = LocalDate.class),
        @Arg(column = "achievement_name", javaType = String.class),
        @Arg(column = "attachment_ids", javaType = String.class),
        @Arg(column = "achievement_status", javaType = String.class),
        @Arg(column = "created_at", javaType = java.time.LocalDateTime.class),
        @Arg(column = "updated_at", javaType = java.time.LocalDateTime.class)
    })
    EmploymentRateAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                achievement_date = #{achievementDate},
                achievement_name = #{achievementName},
                attachment_ids = CAST(#{attachmentIds} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'EMPLOYMENT_RATE'
              AND teacher_user_id = #{actorUserId}
              AND deleted_yn = 'N'
            """)
    int update(EmploymentRateAchievementCommand command);

    @Select("""
            SELECT
                batch_job_id,
                evaluation_year,
                action_type,
                job_status,
                target_count,
                processed_count,
                success_count,
                failure_count,
                requested_at,
                completed_at
            FROM employment_rate_batch_jobs
            WHERE batch_job_id = #{jobId}
            """)
    @ConstructorArgs({
        @Arg(column = "batch_job_id", javaType = String.class, id = true),
        @Arg(column = "evaluation_year", javaType = String.class),
        @Arg(column = "action_type", javaType = String.class),
        @Arg(column = "job_status", javaType = String.class),
        @Arg(column = "target_count", javaType = Integer.class),
        @Arg(column = "processed_count", javaType = Integer.class),
        @Arg(column = "success_count", javaType = Integer.class),
        @Arg(column = "failure_count", javaType = Integer.class),
        @Arg(column = "requested_at", javaType = java.time.LocalDateTime.class),
        @Arg(column = "completed_at", javaType = java.time.LocalDateTime.class)
    })
    EmploymentRateBulkJobResponse findBulkJob(@Param("jobId") String jobId);
}
