package kr.ac.knue.commonfoundation.f3_user_story_2;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis persistence boundary for course-operation rows and required change-history records. */
@Mapper
public interface CourseOperationAchievementMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                owner.login_id AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.occurred_date AS "occurredDate",
                achievement.performance_detail AS "performanceDetail",
                achievement.certification_status AS "certificationStatus",
                achievement.attachment_ids::text AS "attachmentIds"
            FROM course_offering_operation_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            WHERE achievement.deleted_yn = 'N'
            <if test="criteria.normalizedManagementNo() != null">
                AND achievement.management_no = #{criteria.normalizedManagementNo}
            </if>
            <if test="criteria.normalizedTeacherName() != null">
                AND owner.login_id ILIKE CONCAT('%', #{criteria.normalizedTeacherName}, '%')
            </if>
            <if test="criteria.normalizedManagementItemCode() != null">
                AND achievement.management_item_code = #{criteria.normalizedManagementItemCode}
            </if>
            <if test="criteria.normalizedCertificationStatus() != null">
                AND achievement.certification_status = #{criteria.normalizedCertificationStatus}
            </if>
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
            ORDER BY achievement.occurred_date DESC, achievement.achievement_id DESC
            LIMIT #{criteria.safePageSize}
            OFFSET #{criteria.offset}
            </script>
            """)
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM course_offering_operation_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            WHERE achievement.deleted_yn = 'N'
            <if test="criteria.normalizedManagementNo() != null">
                AND achievement.management_no = #{criteria.normalizedManagementNo}
            </if>
            <if test="criteria.normalizedTeacherName() != null">
                AND owner.login_id ILIKE CONCAT('%', #{criteria.normalizedTeacherName}, '%')
            </if>
            <if test="criteria.normalizedManagementItemCode() != null">
                AND achievement.management_item_code = #{criteria.normalizedManagementItemCode}
            </if>
            <if test="criteria.normalizedCertificationStatus() != null">
                AND achievement.certification_status = #{criteria.normalizedCertificationStatus}
            </if>
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
    long count(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                owner.login_id AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.occurred_date AS "occurredDate",
                achievement.performance_detail AS "performanceDetail",
                achievement.certification_status AS "certificationStatus",
                achievement.attachment_ids::text AS "attachmentIds"
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
                achievement.occurred_date AS "occurredDate",
                achievement.performance_detail AS "performanceDetail",
                achievement.certification_status AS "certificationStatus",
                achievement.attachment_ids::text AS "attachmentIds"
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
    long countReadableById(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Insert("""
            INSERT INTO course_offering_operation_achievements (
                management_no,
                target_user_id,
                evaluation_year,
                management_item_code,
                occurred_date,
                performance_detail,
                certification_status,
                attachment_ids,
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
                'DRAFT',
                CAST(#{attachmentIds} AS jsonb),
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
            @Param("attachmentIds") String attachmentIds,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE course_offering_operation_achievements
            SET management_item_code = #{managementItemCode},
                occurred_date = #{achievementDate},
                performance_detail = #{performanceDetails},
                attachment_ids = CAST(#{attachmentIds} AS jsonb),
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
            @Param("attachmentIds") String attachmentIds,
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
                #{achievementId},
                #{changeType},
                'performance_detail',
                #{beforeValue},
                #{afterValue},
                #{actorUserId},
                #{changeReason},
                #{requestId}
            )
            """)
    void insertChangeHistory(
            @Param("achievementId") String achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("actorUserId") Long actorUserId,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
