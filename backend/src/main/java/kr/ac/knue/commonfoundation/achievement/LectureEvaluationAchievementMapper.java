package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import kr.ac.knue.commonfoundation.achievement.LectureEvaluationAchievementModels.Row;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

/**
 * Persists lecture-evaluation headers and their audit/status entries against the BASIC-79
 * foundation tables. Filter SQL is assembled only for supplied criteria to avoid null binds.
 */
@Mapper
public interface LectureEvaluationAchievementMapper {
    @SelectProvider(type = LectureEvaluationAchievementSql.class, method = "list")
    List<Row> list(
            @Param("criteria") LectureEvaluationAchievementModels.SearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("managerScope") boolean managerScope
    );

    @SelectProvider(type = LectureEvaluationAchievementSql.class, method = "count")
    long count(
            @Param("criteria") LectureEvaluationAchievementModels.SearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("managerScope") boolean managerScope
    );

    @Select("""
            SELECT organization_code
            FROM korus_personnel_snapshots
            WHERE employee_no = (
                SELECT employee_no
                FROM users
                WHERE user_id = #{userId}
            )
            ORDER BY updated_at DESC
            LIMIT 1
            """)
    String findOrganizationCodeForUser(@Param("userId") Long userId);

    @Select("""
            SELECT achievement_id AS "achievementId",
                   CONCAT('LE-', achievement_id) AS "managementNo",
                   teacher_user_id AS "teacherUserId",
                   COALESCE(personnel.name, users.login_id) AS "teacherName",
                   organization_code AS "organizationCode",
                   evaluation_year AS "evaluationYear",
                   management_item_code AS "managementItemCode",
                   occurred_date AS "occurredDate",
                   achievement_detail::text AS "achievementDetail",
                   certification_status AS "certificationStatus",
                   attachment_ref AS "attachmentRef",
                   FALSE AS "occurredDateOutOfRangeWarning",
                   achievement.updated_at AS "updatedAt"
            FROM lecture_evaluation_achievements achievement
            JOIN users ON users.user_id = achievement.teacher_user_id
            LEFT JOIN korus_personnel_snapshots personnel
              ON personnel.employee_no = users.employee_no
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.deleted_yn = 'N'
            """)
    Row findById(@Param("achievementId") Long achievementId);

    @Insert("""
            INSERT INTO lecture_evaluation_achievements (
                evaluation_year,
                teacher_user_id,
                organization_code,
                management_item_code,
                occurred_date,
                achievement_detail,
                certification_status,
                attachment_ref,
                created_by,
                updated_by
            )
            VALUES (
                #{request.evaluationYear},
                #{request.teacherUserId},
                #{request.organizationCode},
                #{request.managementItemCode},
                #{request.occurredDate},
                CAST(#{achievementDetail} AS jsonb),
                #{certificationStatus},
                #{request.attachmentRef},
                #{actorUserId},
                #{actorUserId}
            )
            """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "request.achievementId",
            keyColumn = "achievement_id"
    )
    void insert(
            @Param("request") LectureEvaluationSaveRequest request,
            @Param("achievementDetail") String achievementDetail,
            @Param("certificationStatus") String certificationStatus,
            @Param("actorUserId") Long actorUserId
    );

    @Update("""
            UPDATE lecture_evaluation_achievements
            SET management_item_code = #{managementItemCode},
                occurred_date = #{occurredDate},
                achievement_detail = CAST(#{achievementDetail} AS jsonb),
                attachment_ref = #{attachmentRef},
                certification_status = #{certificationStatus},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND deleted_yn = 'N'
            """)
    int update(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") java.time.LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("certificationStatus") String certificationStatus,
            @Param("actorUserId") Long actorUserId
    );

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
                created_by,
                updated_by
            )
            VALUES (
                'LECTURE_EVALUATION',
                #{achievementId},
                #{previousStatus},
                #{nextStatus},
                #{actionType},
                #{reasonCode},
                #{opinion},
                #{actorUserId},
                #{actorUserId},
                #{actorUserId}
            )
            """)
    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus,
            @Param("actionType") String actionType,
            @Param("reasonCode") String reasonCode,
            @Param("opinion") String opinion,
            @Param("actorUserId") Long actorUserId
    );

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
                'lecture_evaluation_achievements',
                #{achievementId}::text,
                #{changeType},
                'achievement_detail',
                #{beforeValue},
                #{afterValue},
                #{actorUserId},
                #{changeReason}
            )
            """)
    void insertChangeHistory(
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("actorUserId") Long actorUserId,
            @Param("changeReason") String changeReason
    );
}
