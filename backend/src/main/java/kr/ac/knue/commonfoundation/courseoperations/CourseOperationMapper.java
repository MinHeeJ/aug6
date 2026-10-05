package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for the COURSE_OPERATION source row, its detail,
 * and required lifecycle/audit side effects.
 */
@Mapper
public interface CourseOperationMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id AS achievementId,
                achievement.management_no AS managementNo,
                achievement.teacher_user_id AS teacherUserId,
                owner.login_id AS teacherName,
                achievement.organization_code AS organizationCode,
                achievement.evaluation_year AS evaluationYear,
                achievement.management_item_code AS managementItemCode,
                achievement.achievement_date AS achievementDate,
                detail.performance_detail AS performanceDetails,
                achievement.achievement_status AS achievementStatus,
                achievement.attachment_refs::text AS attachmentRefs,
                achievement.created_at AS createdAt,
                achievement.updated_at AS updatedAt
            FROM education_achievements achievement
            JOIN course_operation_achievement_details detail
                ON detail.achievement_id = achievement.achievement_id
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_type = 'COURSE_OPERATION'
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
            @Arg(column = "achievementId", javaType = Long.class, id = true),
            @Arg(column = "managementNo", javaType = String.class),
            @Arg(column = "teacherUserId", javaType = Long.class),
            @Arg(column = "teacherName", javaType = String.class),
            @Arg(column = "organizationCode", javaType = String.class),
            @Arg(column = "evaluationYear", javaType = String.class),
            @Arg(column = "managementItemCode", javaType = String.class),
            @Arg(column = "achievementDate", javaType = LocalDate.class),
            @Arg(column = "performanceDetails", javaType = String.class),
            @Arg(column = "achievementStatus", javaType = String.class),
            @Arg(column = "attachmentRefs", javaType = String.class),
            @Arg(column = "createdAt", javaType = java.time.LocalDateTime.class),
            @Arg(column = "updatedAt", javaType = java.time.LocalDateTime.class)
    })
    List<CourseOperationRow> listCourseOperations(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'COURSE_OPERATION'
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
                <otherwise>
                  AND 1 = 0
                </otherwise>
              </choose>
            </script>
            """)
    long countCourseOperations(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            SELECT
                achievement.achievement_id AS achievementId,
                achievement.management_no AS managementNo,
                achievement.teacher_user_id AS teacherUserId,
                owner.login_id AS teacherName,
                achievement.organization_code AS organizationCode,
                achievement.evaluation_year AS evaluationYear,
                achievement.management_item_code AS managementItemCode,
                achievement.achievement_date AS achievementDate,
                detail.performance_detail AS performanceDetails,
                achievement.achievement_status AS achievementStatus,
                achievement.attachment_refs::text AS attachmentRefs,
                achievement.created_at AS createdAt,
                achievement.updated_at AS updatedAt
            FROM education_achievements achievement
            JOIN course_operation_achievement_details detail
                ON detail.achievement_id = achievement.achievement_id
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.achievement_type = 'COURSE_OPERATION'
              AND achievement.deleted_yn = 'N'
            """)
    @ConstructorArgs({
            @Arg(column = "achievementId", javaType = Long.class, id = true),
            @Arg(column = "managementNo", javaType = String.class),
            @Arg(column = "teacherUserId", javaType = Long.class),
            @Arg(column = "teacherName", javaType = String.class),
            @Arg(column = "organizationCode", javaType = String.class),
            @Arg(column = "evaluationYear", javaType = String.class),
            @Arg(column = "managementItemCode", javaType = String.class),
            @Arg(column = "achievementDate", javaType = LocalDate.class),
            @Arg(column = "performanceDetails", javaType = String.class),
            @Arg(column = "achievementStatus", javaType = String.class),
            @Arg(column = "attachmentRefs", javaType = String.class),
            @Arg(column = "createdAt", javaType = java.time.LocalDateTime.class),
            @Arg(column = "updatedAt", javaType = java.time.LocalDateTime.class)
    })
    CourseOperationRow findCourseOperation(@Param("achievementId") Long achievementId);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.achievement_type = 'COURSE_OPERATION'
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
                <otherwise>
                  AND 1 = 0
                </otherwise>
              </choose>
            </script>
            """)
    long countCourseOperationAccess(
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
                management_no,
                achievement_type,
                teacher_user_id,
                organization_code,
                evaluation_year,
                management_item_code,
                achievement_date,
                achievement_status,
                attachment_refs,
                deleted_yn,
                created_by,
                updated_by
            )
            VALUES (
                #{row.managementNo},
                'COURSE_OPERATION',
                #{row.teacherUserId},
                #{row.organizationCode},
                #{row.evaluationYear},
                #{row.managementItemCode},
                #{row.achievementDate},
                'DRAFT',
                CAST(#{row.attachmentRefs} AS jsonb),
                'N',
                #{row.createdBy},
                #{row.createdBy}
            )
            RETURNING achievement_id
            """)
    Long insertAchievement(@Param("row") CourseOperationCreateRow row);

    @Insert("""
            INSERT INTO course_operation_achievement_details (
                achievement_id,
                performance_detail,
                created_by,
                updated_by
            )
            VALUES (
                #{achievementId},
                #{performanceDetails},
                #{actorUserId},
                #{actorUserId}
            )
            """)
    void insertDetails(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                evaluation_year = #{evaluationYear},
                achievement_date = #{achievementDate},
                attachment_refs = CAST(#{attachmentRefs} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'COURSE_OPERATION'
              AND deleted_yn = 'N'
            """)
    void updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE course_operation_achievement_details
            SET performance_detail = #{performanceDetails},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
            """)
    void updateDetails(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails,
            @Param("actorUserId") Long actorUserId);

    @Insert("""
            INSERT INTO education_achievement_status_histories (
                achievement_type,
                achievement_id,
                previous_status,
                next_status,
                action_type,
                reason_code,
                opinion,
                processed_by,
                processed_at
            )
            VALUES (
                #{history.achievementType},
                #{history.achievementId},
                #{history.previousStatus},
                #{history.nextStatus},
                #{history.actionType},
                #{history.reasonCode},
                #{history.opinion},
                #{history.processedBy},
                #{history.processedAt}
            )
            """)
    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);

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
                #{targetBusiness},
                #{targetKey},
                #{changeType},
                #{fieldName},
                #{beforeValue},
                #{afterValue},
                #{changedBy},
                #{changeReason}
            )
            """)
    void insertChangeHistory(
            @Param("targetBusiness") String targetBusiness,
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);
}
