package kr.ac.knue.commonfoundation.courseoperation;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for the course-offering-operation source rows and
 * their mandatory data-change history entries.
 */
@Mapper
public interface CourseOperationMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                owner.login_id AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.achievement_date AS "achievementDate",
                achievement.performance_details AS "performanceDetails",
                achievement.attachment_ids::text AS "attachmentIdsJson",
                achievement.achievement_status AS "achievementStatus",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM course_offering_operation_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            WHERE achievement.deleted_yn = 'N'
            <choose>
                <when test="roles.contains('R01')">
                    AND achievement.target_user_id = #{requesterUserId}
                </when>
                <when test="roles.contains('R02')">
                    AND EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                          AND requester_mapping.status = 'ACTIVE'
                          AND target_mapping.user_id = achievement.target_user_id
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
                          AND target_mapping.user_id = achievement.target_user_id
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
    List<CourseOperationRow> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM course_offering_operation_achievements achievement
            WHERE achievement.deleted_yn = 'N'
            <choose>
                <when test="roles.contains('R01')">
                    AND achievement.target_user_id = #{requesterUserId}
                </when>
                <when test="roles.contains('R02')">
                    AND EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                          AND requester_mapping.status = 'ACTIVE'
                          AND target_mapping.user_id = achievement.target_user_id
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
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                </when>
                <otherwise>
                    AND 1 = 0
                </otherwise>
            </choose>
            </script>
            """)
    long count(@Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);

    @Select("""
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                owner.login_id AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.achievement_date AS "achievementDate",
                achievement.performance_details AS "performanceDetails",
                achievement.attachment_ids::text AS "attachmentIdsJson",
                achievement.achievement_status AS "achievementStatus",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM course_offering_operation_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.deleted_yn = 'N'
            """)
    CourseOperationRow findById(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                owner.login_id AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.achievement_date AS "achievementDate",
                achievement.performance_details AS "performanceDetails",
                achievement.attachment_ids::text AS "attachmentIdsJson",
                achievement.achievement_status AS "achievementStatus",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM course_offering_operation_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            WHERE achievement.management_no = #{managementNo}
              AND achievement.deleted_yn = 'N'
            """)
    CourseOperationRow findByManagementNo(@Param("managementNo") String managementNo);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM course_offering_operation_achievements achievement
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.deleted_yn = 'N'
            <choose>
                <when test="roles.contains('R01')">
                    AND achievement.target_user_id = #{requesterUserId}
                </when>
                <when test="roles.contains('R02')">
                    AND EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                          AND requester_mapping.status = 'ACTIVE'
                          AND target_mapping.user_id = achievement.target_user_id
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
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                </when>
                <otherwise>
                    AND 1 = 0
                </otherwise>
            </choose>
            </script>
            """)
    int countAccessible(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Insert("""
            INSERT INTO course_offering_operation_achievements (
                management_no,
                target_user_id,
                evaluation_year,
                management_item_code,
                achievement_date,
                performance_details,
                attachment_ids,
                achievement_status,
                deleted_yn,
                created_by,
                updated_by
            )
            VALUES (
                #{managementNo},
                #{targetUserId},
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{performanceDetails},
                CAST(#{attachmentIdsJson} AS jsonb),
                'DRAFT',
                'N',
                #{actorUserId},
                #{actorUserId}
            )
            """)
    void insert(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("performanceDetails") String performanceDetails,
            @Param("attachmentIdsJson") String attachmentIdsJson,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE course_offering_operation_achievements
            SET management_item_code = #{managementItemCode},
                achievement_date = #{achievementDate},
                performance_details = #{performanceDetails},
                attachment_ids = CAST(#{attachmentIdsJson} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND deleted_yn = 'N'
            """)
    int update(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("performanceDetails") String performanceDetails,
            @Param("attachmentIdsJson") String attachmentIdsJson,
            @Param("actorUserId") Long actorUserId);

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
                'course_offering_operation_achievements',
                #{targetKey},
                #{changeType},
                'performance_details',
                #{beforeValue},
                #{afterValue},
                #{changedBy},
                #{changeReason},
                #{requestId}
            )
            """)
    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
