package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Persists and reads lecture-evaluation rows plus their audit and certification-status histories.
 */
@Mapper
public interface EducationAchievementMapper {
    @Select("""
            <script>
            SELECT lea.achievement_id AS "achievementId",
                   CONCAT('LE-', lea.achievement_id) AS "managementNo",
                   COALESCE(kps.name, u.login_id) AS "teacherName",
                   lea.management_item_code AS "managementItemCode",
                   lea.occurred_date AS "occurredDate",
                   lea.certification_status AS "certificationStatus",
                   (lea.attachment_ref IS NOT NULL) AS "attachmentExists",
                   lea.achievement_detail::text AS "achievementDetail",
                   lea.attachment_ref AS "attachmentRef",
                   lea.evaluation_year AS "evaluationYear",
                   lea.target_user_id AS "targetUserId",
                   lea.organization_code AS "organizationCode"
            FROM lecture_evaluation_achievements lea
            JOIN users u ON u.user_id = lea.target_user_id
            LEFT JOIN korus_personnel_snapshots kps ON kps.employee_no = u.employee_no
            WHERE lea.deleted_yn = 'N'
            <if test="criteria.managementNo != null and criteria.managementNo != ''">
              AND CAST(lea.achievement_id AS varchar) ILIKE CONCAT('%', #{criteria.managementNo}, '%')
            </if>
            <if test="criteria.teacherName != null and criteria.teacherName != ''">
              AND COALESCE(kps.name, u.login_id) ILIKE CONCAT('%', #{criteria.teacherName}, '%')
            </if>
            <if test="criteria.managementItemCode != null and criteria.managementItemCode != ''">
              AND lea.management_item_code = #{criteria.managementItemCode}
            </if>
            <if test="criteria.occurredDateFrom != null">
              AND lea.occurred_date <![CDATA[>=]]> #{criteria.occurredDateFrom}
            </if>
            <if test="criteria.occurredDateTo != null">
              AND lea.occurred_date <![CDATA[<=]]> #{criteria.occurredDateTo}
            </if>
            <if test="criteria.certificationStatus != null and criteria.certificationStatus != ''">
              AND lea.certification_status = #{criteria.certificationStatus}
            </if>
            ORDER BY lea.occurred_date DESC, lea.achievement_id DESC
            LIMIT #{criteria.size} OFFSET #{criteria.offset}
            </script>
            """)
    List<Map<String, Object>> listLectureEvaluationAchievements(
            @Param("criteria") LectureEvaluationAchievementSearchCriteria criteria
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM lecture_evaluation_achievements lea
            JOIN users u ON u.user_id = lea.target_user_id
            LEFT JOIN korus_personnel_snapshots kps ON kps.employee_no = u.employee_no
            WHERE lea.deleted_yn = 'N'
            <if test="criteria.managementNo != null and criteria.managementNo != ''">
              AND CAST(lea.achievement_id AS varchar) ILIKE CONCAT('%', #{criteria.managementNo}, '%')
            </if>
            <if test="criteria.teacherName != null and criteria.teacherName != ''">
              AND COALESCE(kps.name, u.login_id) ILIKE CONCAT('%', #{criteria.teacherName}, '%')
            </if>
            <if test="criteria.managementItemCode != null and criteria.managementItemCode != ''">
              AND lea.management_item_code = #{criteria.managementItemCode}
            </if>
            <if test="criteria.occurredDateFrom != null">
              AND lea.occurred_date <![CDATA[>=]]> #{criteria.occurredDateFrom}
            </if>
            <if test="criteria.occurredDateTo != null">
              AND lea.occurred_date <![CDATA[<=]]> #{criteria.occurredDateTo}
            </if>
            <if test="criteria.certificationStatus != null and criteria.certificationStatus != ''">
              AND lea.certification_status = #{criteria.certificationStatus}
            </if>
            </script>
            """)
    long countLectureEvaluationAchievements(
            @Param("criteria") LectureEvaluationAchievementSearchCriteria criteria
    );

    @Select("""
            SELECT organization_code
            FROM organization_user_mappings
            WHERE user_id = #{userId}
              AND mapping_type = 'ORGANIZATION'
              AND status = 'ACTIVE'
            ORDER BY created_at DESC
            LIMIT 1
            """)
    String findActiveOrganizationCodeForUser(@Param("userId") Long userId);

    @Select("""
            SELECT achievement_id AS "achievementId", evaluation_year AS "evaluationYear",
                   target_user_id AS "targetUserId", organization_code AS "organizationCode",
                   management_item_code AS "managementItemCode", occurred_date AS "occurredDate",
                   achievement_detail::text AS "achievementDetail", certification_status AS "certificationStatus",
                   attachment_ref AS "attachmentRef"
            FROM lecture_evaluation_achievements
            WHERE achievement_id = #{achievementId} AND deleted_yn = 'N'
            """)
    Map<String, Object> findLectureEvaluationAchievement(@Param("achievementId") Long achievementId);

    @Insert("""
            INSERT INTO lecture_evaluation_achievements (
                evaluation_year, target_user_id, organization_code, management_item_code, occurred_date,
                achievement_detail, certification_status, attachment_ref, created_by, updated_by
            ) VALUES (
                #{evaluationYear}, #{userId}, #{organizationCode}, #{request.managementItemCode}, #{request.occurredDate},
                CAST(#{request.achievementDetailJson} AS jsonb), 'DRAFTING', #{request.attachmentRef}, #{userId}, #{userId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "request.achievementId", keyColumn = "achievement_id")
    void insertLectureEvaluationAchievement(
            @Param("request") LectureEvaluationAchievementRequest request,
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("userId") Long userId
    );

    @Update("""
            UPDATE lecture_evaluation_achievements
            SET management_item_code = #{request.managementItemCode},
                occurred_date = #{request.occurredDate},
                achievement_detail = CAST(#{request.achievementDetailJson} AS jsonb),
                attachment_ref = #{request.attachmentRef},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{userId}
            WHERE achievement_id = #{request.achievementId} AND deleted_yn = 'N'
            """)
    int updateLectureEvaluationAchievement(
            @Param("request") LectureEvaluationAchievementRequest request,
            @Param("userId") Long userId
    );

    @Update("""
            UPDATE lecture_evaluation_achievements
            SET certification_status = #{nextStatus}, updated_at = CURRENT_TIMESTAMP, updated_by = #{userId}
            WHERE achievement_id = #{achievementId} AND certification_status = #{previousStatus} AND deleted_yn = 'N'
            """)
    int updateLectureEvaluationStatus(
            @Param("achievementId") Long achievementId,
            @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus,
            @Param("userId") Long userId
    );

    @Insert("""
            INSERT INTO education_achievement_status_histories (
                achievement_type, achievement_id, previous_status, next_status, action_type,
                reason_code, opinion, processed_by, processed_at
            ) VALUES (
                #{transition.achievementType}, #{transition.achievementId}, #{transition.previousStatus},
                #{transition.nextStatus}, #{transition.actionType}, #{transition.reasonCode},
                #{transition.opinion}, #{transition.processedBy}, #{transition.processedAt}
            )
            """)
    void insertStatusHistory(@Param("transition") EducationAchievementStatusTransition transition);

    @Insert("""
            INSERT INTO data_change_histories (
                target_business, target_key, change_type, field_name, before_value, after_value,
                changed_by, change_reason
            ) VALUES (
                #{targetBusiness}, #{targetKey}, #{changeType}, #{fieldName}, #{beforeValue}, #{afterValue},
                #{changedBy}, #{changeReason}
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
            @Param("changeReason") String changeReason
    );
}
