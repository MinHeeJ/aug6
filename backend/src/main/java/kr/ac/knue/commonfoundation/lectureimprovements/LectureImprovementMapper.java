package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for lecture-improvement source rows and their
 * required data-change history records.
 */
@Mapper
public interface LectureImprovementMapper {
    @Select("""
            SELECT
                achievement_id AS achievementId,
                target_user_id AS targetUserId,
                (
                    SELECT owner.login_id
                    FROM users owner
                    WHERE owner.user_id = target_user_id
                ) AS teacherName,
                management_no AS managementNo,
                management_item_code AS managementItemCode,
                achievement_date AS achievementDate,
                achievement_content AS achievementContent,
                academic_year AS academicYear,
                semester,
                achievement_status AS achievementStatus,
                attachment_refs AS attachmentRefs,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM teaching_improvement_achievements
            WHERE deleted_yn = 'N'
              AND target_user_id = #{userId}
            ORDER BY achievement_date DESC, achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            """)
    List<LectureImprovementAchievement> listForOwner(
            @Param("userId") Long userId,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*)
            FROM teaching_improvement_achievements
            WHERE deleted_yn = 'N'
              AND target_user_id = #{userId}
            """)
    long countForOwner(@Param("userId") Long userId);

    @Select("""
            SELECT
                achievement_id AS achievementId,
                target_user_id AS targetUserId,
                (
                    SELECT owner.login_id
                    FROM users owner
                    WHERE owner.user_id = target_user_id
                ) AS teacherName,
                management_no AS managementNo,
                management_item_code AS managementItemCode,
                achievement_date AS achievementDate,
                achievement_content AS achievementContent,
                academic_year AS academicYear,
                semester,
                achievement_status AS achievementStatus,
                attachment_refs AS attachmentRefs,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM teaching_improvement_achievements
            WHERE achievement_id = #{achievementId}
              AND deleted_yn = 'N'
            """)
    LectureImprovementAchievement findById(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT
                achievement_id AS achievementId,
                target_user_id AS targetUserId,
                (
                    SELECT owner.login_id
                    FROM users owner
                    WHERE owner.user_id = target_user_id
                ) AS teacherName,
                management_no AS managementNo,
                management_item_code AS managementItemCode,
                achievement_date AS achievementDate,
                achievement_content AS achievementContent,
                academic_year AS academicYear,
                semester,
                achievement_status AS achievementStatus,
                attachment_refs AS attachmentRefs,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM teaching_improvement_achievements
            WHERE management_no = #{managementNo}
              AND deleted_yn = 'N'
            """)
    LectureImprovementAchievement findByManagementNo(@Param("managementNo") String managementNo);

    @Insert("""
            INSERT INTO teaching_improvement_achievements (
                target_user_id,
                management_no,
                management_item_code,
                achievement_date,
                achievement_content,
                academic_year,
                semester,
                achievement_status,
                attachment_refs,
                deleted_yn,
                created_by,
                updated_by
            )
            VALUES (
                #{userId},
                #{managementNo},
                #{request.managementItemCode},
                #{request.achievementDate},
                #{request.achievementContent},
                #{request.academicYear},
                #{request.semester},
                'DRAFT',
                #{attachmentRefs},
                'N',
                #{userId},
                #{userId}
            )
            """)
    void insert(
            @Param("userId") Long userId,
            @Param("managementNo") String managementNo,
            @Param("request") LectureImprovementRequest request,
            @Param("attachmentRefs") String attachmentRefs);

    @Update("""
            UPDATE teaching_improvement_achievements
            SET management_item_code = #{request.managementItemCode},
                achievement_date = #{request.achievementDate},
                achievement_content = #{request.achievementContent},
                academic_year = #{request.academicYear},
                semester = #{request.semester},
                attachment_refs = #{attachmentRefs},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{userId}
            WHERE achievement_id = #{achievementId}
              AND target_user_id = #{userId}
              AND deleted_yn = 'N'
            """)
    int update(
            @Param("achievementId") Long achievementId,
            @Param("userId") Long userId,
            @Param("request") LectureImprovementRequest request,
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
                change_reason
            )
            VALUES (
                'teaching_improvement_achievements',
                #{achievementId},
                #{changeType},
                'lecture_improvement',
                #{beforeValue},
                #{afterValue},
                #{userId},
                #{changeReason}
            )
            """)
    void insertChangeHistory(
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("userId") Long userId,
            @Param("changeReason") String changeReason);
}
