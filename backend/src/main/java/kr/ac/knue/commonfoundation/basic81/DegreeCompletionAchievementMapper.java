package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Maps degree-completion headers, student details, and required audit records to
 * the PostgreSQL persistence contract.
 */
@Mapper
public interface DegreeCompletionAchievementMapper {
    @Select("""
            <script>
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                COALESCE(personnel.name, owner.login_id) AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.occurred_date AS "occurredDate",
                achievement.achievement_detail AS "achievementDetail",
                achievement.certification_status AS "certificationStatus",
                achievement.attachment_ref AS "attachmentRef",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM degree_completion_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            LEFT JOIN korus_personnel_snapshots personnel
                ON personnel.employee_no = owner.employee_no
            WHERE achievement.deleted_yn = 'N'
              AND (
                    achievement.target_user_id = #{requesterUserId}
                    <if test="roles != null and roles.contains('R02')">
                    OR EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                          AND requester_mapping.status = 'ACTIVE'
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                    </if>
                    <if test="roles != null and roles.contains('R04')">
                    OR EXISTS (
                        SELECT 1
                        FROM evaluation_organization_mappings scope_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = scope_mapping.organization_code
                        WHERE scope_mapping.user_id = #{requesterUserId}
                          AND scope_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                          AND scope_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                    </if>
              )
            <if test="criteria.normalizedManagementNo() != null">
              AND achievement.management_no ILIKE CONCAT('%', #{criteria.managementNo}, '%')
            </if>
            <if test="criteria.normalizedTeacherName() != null">
              AND COALESCE(personnel.name, owner.login_id) ILIKE CONCAT('%', #{criteria.teacherName}, '%')
            </if>
            <if test="criteria.normalizedCertificationStatus() != null">
              AND achievement.certification_status = #{criteria.certificationStatus}
            </if>
            ORDER BY achievement.occurred_date DESC, achievement.achievement_id DESC
            LIMIT #{criteria.safePageSize}
            OFFSET #{criteria.offset}
            </script>
            """)
    List<DegreeCompletionAchievementHeaderRow> list(
            @Param("criteria") DegreeCompletionAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM degree_completion_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            LEFT JOIN korus_personnel_snapshots personnel
                ON personnel.employee_no = owner.employee_no
            WHERE achievement.deleted_yn = 'N'
              AND (
                    achievement.target_user_id = #{requesterUserId}
                    <if test="roles != null and roles.contains('R02')">
                    OR EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = #{requesterUserId}
                          AND requester_mapping.status = 'ACTIVE'
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                    </if>
                    <if test="roles != null and roles.contains('R04')">
                    OR EXISTS (
                        SELECT 1
                        FROM evaluation_organization_mappings scope_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = scope_mapping.organization_code
                        WHERE scope_mapping.user_id = #{requesterUserId}
                          AND scope_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                          AND scope_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                    </if>
              )
            <if test="criteria.normalizedManagementNo() != null">
              AND achievement.management_no ILIKE CONCAT('%', #{criteria.managementNo}, '%')
            </if>
            <if test="criteria.normalizedTeacherName() != null">
              AND COALESCE(personnel.name, owner.login_id) ILIKE CONCAT('%', #{criteria.teacherName}, '%')
            </if>
            <if test="criteria.normalizedCertificationStatus() != null">
              AND achievement.certification_status = #{criteria.certificationStatus}
            </if>
            </script>
            """)
    long count(
            @Param("criteria") DegreeCompletionAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                COALESCE(personnel.name, owner.login_id) AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.occurred_date AS "occurredDate",
                achievement.achievement_detail AS "achievementDetail",
                achievement.certification_status AS "certificationStatus",
                achievement.attachment_ref AS "attachmentRef",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM degree_completion_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            LEFT JOIN korus_personnel_snapshots personnel
                ON personnel.employee_no = owner.employee_no
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.deleted_yn = 'N'
            """)
    DegreeCompletionAchievementHeaderRow findHeader(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT
                achievement.achievement_id AS "achievementId",
                achievement.management_no AS "managementNo",
                achievement.target_user_id AS "targetUserId",
                COALESCE(personnel.name, owner.login_id) AS "teacherName",
                achievement.evaluation_year AS "evaluationYear",
                achievement.management_item_code AS "managementItemCode",
                achievement.occurred_date AS "occurredDate",
                achievement.achievement_detail AS "achievementDetail",
                achievement.certification_status AS "certificationStatus",
                achievement.attachment_ref AS "attachmentRef",
                achievement.created_at AS "createdAt",
                achievement.updated_at AS "updatedAt"
            FROM degree_completion_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.target_user_id
            LEFT JOIN korus_personnel_snapshots personnel
                ON personnel.employee_no = owner.employee_no
            WHERE achievement.management_no = #{managementNo}
              AND achievement.deleted_yn = 'N'
            """)
    DegreeCompletionAchievementHeaderRow findHeaderByManagementNo(
            @Param("managementNo") String managementNo);

    @Select("""
            SELECT
                degree_completion_student_id AS "degreeCompletionStudentId",
                achievement_id AS "achievementId",
                degree_type AS "degreeType",
                student_name AS "studentName",
                thesis_title AS "thesisTitle",
                degree_awarded_date AS "degreeAwardedDate"
            FROM degree_completion_students
            WHERE achievement_id = #{achievementId}
            ORDER BY degree_awarded_date, degree_completion_student_id
            """)
    List<DegreeCompletionStudent> findStudents(@Param("achievementId") Long achievementId);

    @Insert("""
            INSERT INTO degree_completion_achievements (
                management_no,
                target_user_id,
                evaluation_year,
                management_item_code,
                occurred_date,
                achievement_detail,
                attachment_ref,
                created_by,
                updated_by
            )
            VALUES (
                #{managementNo},
                #{targetUserId},
                #{evaluationYear},
                #{managementItemCode},
                #{occurredDate},
                #{achievementDetail},
                #{attachmentRef},
                #{createdBy},
                #{createdBy}
            )
            """)
    void insertHeader(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy);

    @Update("""
            UPDATE degree_completion_achievements
            SET management_item_code = #{managementItemCode},
                occurred_date = #{occurredDate},
                achievement_detail = #{achievementDetail},
                attachment_ref = #{attachmentRef},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{updatedBy}
            WHERE achievement_id = #{achievementId}
              AND deleted_yn = 'N'
            """)
    void updateHeader(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy);

    @Delete("DELETE FROM degree_completion_students WHERE achievement_id = #{achievementId}")
    void deleteStudents(@Param("achievementId") Long achievementId);

    @Insert("""
            INSERT INTO degree_completion_students (
                achievement_id,
                degree_type,
                student_name,
                thesis_title,
                degree_awarded_date,
                created_by,
                updated_by
            )
            VALUES (
                #{achievementId},
                #{student.degreeType},
                #{student.studentName},
                #{student.thesisTitle},
                #{student.degreeAwardedDate},
                #{userId},
                #{userId}
            )
            """)
    void insertStudent(
            @Param("achievementId") Long achievementId,
            @Param("student") DegreeCompletionStudentRequest student,
            @Param("userId") Long userId);

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
                change_reason,
                request_id
            )
            VALUES (
                #{targetBusiness},
                #{targetKey},
                #{changeType},
                #{fieldName},
                #{beforeValue},
                #{afterValue},
                #{changedBy},
                #{changeReason},
                #{requestId}
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
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
