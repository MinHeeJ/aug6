package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for the BASIC-83 employment-rate-improvement
 * master rows, their typed detail rows, and required audit records.
 */
@Mapper
public interface EmploymentRateImprovementAchievementMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id,
                achievement.teacher_user_id,
                owner.login_id AS teacher_login_id,
                achievement.organization_code,
                achievement.evaluation_year,
                achievement.management_item_code,
                achievement.achievement_date,
                achievement.achievement_name,
                achievement.attachment_ids::text AS attachment_ids_json,
                achievement.achievement_status,
                detail.special_lecture_start_date,
                detail.special_lecture_end_date,
                detail.mock_exam_question_period,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            JOIN employment_rate_improvement_achievement_details detail
                ON detail.achievement_id = achievement.achievement_id
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
            <choose>
                <when test="roles != null and roles.contains('R01')">
                    AND achievement.teacher_user_id = #{requesterUserId}
                </when>
                <when test="roles != null and roles.contains('R02')">
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
                <when test="roles != null and roles.contains('R04')">
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
        @Arg(column = "teacher_user_id", javaType = Long.class),
        @Arg(column = "teacher_login_id", javaType = String.class),
        @Arg(column = "organization_code", javaType = String.class),
        @Arg(column = "evaluation_year", javaType = String.class),
        @Arg(column = "management_item_code", javaType = String.class),
        @Arg(column = "achievement_date", javaType = LocalDate.class),
        @Arg(column = "achievement_name", javaType = String.class),
        @Arg(column = "attachment_ids_json", javaType = String.class),
        @Arg(column = "achievement_status", javaType = String.class),
        @Arg(column = "special_lecture_start_date", javaType = LocalDate.class),
        @Arg(column = "special_lecture_end_date", javaType = LocalDate.class),
        @Arg(column = "mock_exam_question_period", javaType = String.class),
        @Arg(column = "created_at", javaType = LocalDateTime.class),
        @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    List<EmploymentRateImprovementAchievementEntity> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
            <choose>
                <when test="roles != null and roles.contains('R01')">
                    AND achievement.teacher_user_id = #{requesterUserId}
                </when>
                <when test="roles != null and roles.contains('R02')">
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
                <when test="roles != null and roles.contains('R04')">
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
                <otherwise>
                    AND 1 = 0
                </otherwise>
            </choose>
            </script>
            """)
    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            <script>
            SELECT
                achievement.achievement_id,
                achievement.teacher_user_id,
                owner.login_id AS teacher_login_id,
                achievement.organization_code,
                achievement.evaluation_year,
                achievement.management_item_code,
                achievement.achievement_date,
                achievement.achievement_name,
                achievement.attachment_ids::text AS attachment_ids_json,
                achievement.achievement_status,
                detail.special_lecture_start_date,
                detail.special_lecture_end_date,
                detail.mock_exam_question_period,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            JOIN employment_rate_improvement_achievement_details detail
                ON detail.achievement_id = achievement.achievement_id
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
            <choose>
                <when test="roles != null and roles.contains('R01')">
                    AND achievement.teacher_user_id = #{requesterUserId}
                </when>
                <when test="roles != null and roles.contains('R02')">
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
                <when test="roles != null and roles.contains('R04')">
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
                <otherwise>
                    AND 1 = 0
                </otherwise>
            </choose>
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "achievement_id", javaType = Long.class, id = true),
        @Arg(column = "teacher_user_id", javaType = Long.class),
        @Arg(column = "teacher_login_id", javaType = String.class),
        @Arg(column = "organization_code", javaType = String.class),
        @Arg(column = "evaluation_year", javaType = String.class),
        @Arg(column = "management_item_code", javaType = String.class),
        @Arg(column = "achievement_date", javaType = LocalDate.class),
        @Arg(column = "achievement_name", javaType = String.class),
        @Arg(column = "attachment_ids_json", javaType = String.class),
        @Arg(column = "achievement_status", javaType = String.class),
        @Arg(column = "special_lecture_start_date", javaType = LocalDate.class),
        @Arg(column = "special_lecture_end_date", javaType = LocalDate.class),
        @Arg(column = "mock_exam_question_period", javaType = String.class),
        @Arg(column = "created_at", javaType = LocalDateTime.class),
        @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    EmploymentRateImprovementAchievementEntity findScoped(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            SELECT mapping.organization_code
            FROM organization_user_mappings mapping
            WHERE mapping.user_id = #{userId}
              AND mapping.status = 'ACTIVE'
            ORDER BY mapping.created_at ASC
            LIMIT 1
            """)
    String findActiveOrganizationCode(@Param("userId") Long userId);

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
            VALUES (
                'EMPLOYMENT_RATE_IMPROVEMENT',
                #{teacherUserId},
                #{organizationCode},
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                NULL,
                CAST(#{attachmentIdsJson} AS jsonb),
                'DRAFT',
                'N',
                #{actorUserId},
                #{actorUserId}
            )
            RETURNING achievement_id
            """)
    Long insertAchievement(
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIdsJson") String attachmentIdsJson,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            INSERT INTO employment_rate_improvement_achievement_details (
                achievement_id,
                special_lecture_start_date,
                special_lecture_end_date,
                mock_exam_question_period
            )
            VALUES (
                #{achievementId},
                #{specialLectureStartDate},
                #{specialLectureEndDate},
                #{mockExamQuestionPeriod}
            )
            """)
    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                evaluation_year = EXTRACT(YEAR FROM #{achievementDate})::varchar,
                achievement_date = #{achievementDate},
                attachment_ids = CAST(#{attachmentIdsJson} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND deleted_yn = 'N'
            """)
    void updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIdsJson") String attachmentIdsJson,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE employment_rate_improvement_achievement_details
            SET special_lecture_start_date = #{specialLectureStartDate},
                special_lecture_end_date = #{specialLectureEndDate},
                mock_exam_question_period = #{mockExamQuestionPeriod}
            WHERE achievement_id = #{achievementId}
            """)
    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod);

    @Update("""
            INSERT INTO education_achievement_status_histories (
                achievement_type,
                achievement_id,
                previous_status,
                next_status,
                action_type,
                opinion,
                processed_by,
                processed_at
            )
            VALUES (
                'EMPLOYMENT_RATE_IMPROVEMENT',
                #{achievementId},
                NULL,
                'DRAFT',
                'CREATE',
                #{opinion},
                #{actorUserId},
                CURRENT_TIMESTAMP
            )
            """)
    void insertCreateStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("opinion") String opinion,
            @Param("actorUserId") Long actorUserId);

    @Update("""
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
                'employment_rate_improvement',
                #{beforeValue},
                #{afterValue},
                #{actorUserId},
                #{changeReason},
                #{requestId}
            )
            """)
    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("actorUserId") Long actorUserId,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
