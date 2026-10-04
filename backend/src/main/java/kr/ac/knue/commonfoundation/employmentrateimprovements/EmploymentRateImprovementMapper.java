package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for BASIC-83 employment-rate improvement headers,
 * details, and audit history. The detail table is required by the approved data model.
 */
@Mapper
public interface EmploymentRateImprovementMapper {
    @Select("""
            SELECT achievement.achievement_id,
                   achievement.management_no,
                   achievement.teacher_user_id,
                   owner.login_id AS teacher_login_id,
                   achievement.organization_code,
                   achievement.evaluation_year,
                   achievement.management_item_code,
                   achievement.achievement_date,
                   achievement.achievement_status,
                   detail.special_lecture_start_date,
                   detail.special_lecture_end_date,
                   detail.mock_exam_question_period,
                   achievement.attachment_ref,
                   achievement.created_at,
                   achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
              ON owner.user_id = achievement.teacher_user_id
            JOIN employment_rate_improvement_achievement_details detail
              ON detail.achievement_id = achievement.achievement_id
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
              AND achievement.teacher_user_id = #{teacherUserId}
            ORDER BY achievement.achievement_date DESC,
                     achievement.achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            """)
    List<EmploymentRateImprovementRow> listForOwner(
            @Param("teacherUserId") Long teacherUserId,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
              AND achievement.teacher_user_id = #{teacherUserId}
            """)
    long countForOwner(@Param("teacherUserId") Long teacherUserId);

    @Select("""
            SELECT achievement.achievement_id,
                   achievement.management_no,
                   achievement.teacher_user_id,
                   owner.login_id AS teacher_login_id,
                   achievement.organization_code,
                   achievement.evaluation_year,
                   achievement.management_item_code,
                   achievement.achievement_date,
                   achievement.achievement_status,
                   detail.special_lecture_start_date,
                   detail.special_lecture_end_date,
                   detail.mock_exam_question_period,
                   achievement.attachment_ref,
                   achievement.created_at,
                   achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
              ON owner.user_id = achievement.teacher_user_id
            JOIN employment_rate_improvement_achievement_details detail
              ON detail.achievement_id = achievement.achievement_id
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
            ORDER BY achievement.achievement_date DESC,
                     achievement.achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            """)
    List<EmploymentRateImprovementRow> listForReaders(
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND achievement.deleted_yn = 'N'
            """)
    long countForReaders();

    @Select("""
            SELECT achievement.achievement_id,
                   achievement.management_no,
                   achievement.teacher_user_id,
                   owner.login_id AS teacher_login_id,
                   achievement.organization_code,
                   achievement.evaluation_year,
                   achievement.management_item_code,
                   achievement.achievement_date,
                   achievement.achievement_status,
                   detail.special_lecture_start_date,
                   detail.special_lecture_end_date,
                   detail.mock_exam_question_period,
                   achievement.attachment_ref,
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
            """)
    EmploymentRateImprovementRow findById(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT mapping.organization_code
            FROM organization_user_mappings mapping
            WHERE mapping.user_id = #{userId}
              AND mapping.status = 'ACTIVE'
            ORDER BY mapping.organization_code
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
                attachment_ref,
                achievement_status,
                deleted_yn,
                created_by,
                updated_by
            )
            VALUES (
                #{managementNo},
                'EMPLOYMENT_RATE_IMPROVEMENT',
                #{teacherUserId},
                #{organizationCode},
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{attachmentRef},
                'DRAFT',
                'N',
                #{actorUserId},
                #{actorUserId}
            )
            RETURNING achievement_id
            """)
    long insertHeader(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRef") String attachmentRef,
            @Param("actorUserId") Long actorUserId);

    @Insert("""
            INSERT INTO employment_rate_improvement_achievement_details (
                achievement_id,
                special_lecture_start_date,
                special_lecture_end_date,
                mock_exam_question_period,
                created_by,
                updated_by
            )
            VALUES (
                #{achievementId},
                #{specialLectureStartDate},
                #{specialLectureEndDate},
                #{mockExamQuestionPeriod},
                #{actorUserId},
                #{actorUserId}
            )
            """)
    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE education_achievements
            SET evaluation_year = #{evaluationYear},
                management_item_code = #{managementItemCode},
                achievement_date = #{achievementDate},
                attachment_ref = #{attachmentRef},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'EMPLOYMENT_RATE_IMPROVEMENT'
              AND deleted_yn = 'N'
            """)
    int updateHeader(
            @Param("achievementId") Long achievementId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRef") String attachmentRef,
            @Param("actorUserId") Long actorUserId);

    @Update("""
            UPDATE employment_rate_improvement_achievement_details
            SET special_lecture_start_date = #{specialLectureStartDate},
                special_lecture_end_date = #{specialLectureEndDate},
                mock_exam_question_period = #{mockExamQuestionPeriod},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
            """)
    int updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
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
                'education_achievements',
                #{achievementId},
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
            @Param("achievementId") String achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("actorUserId") Long actorUserId,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
