package com.example.faculty.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for lecture-evaluation source rows and their required audit histories.
 */
@Mapper
public interface LectureEvaluationAchievementMapper {
    @Select("""
            <script>
            SELECT a.achievement_id AS \"achievementId\",
                   a.management_no AS \"managementNo\",
                   a.teacher_user_id AS \"teacherUserId\",
                   personnel.name AS \"teacherName\",
                   a.evaluation_year AS \"evaluationYear\",
                   a.organization_code AS \"organizationCode\",
                   a.management_item_code AS \"managementItemCode\",
                   a.occurred_date AS \"occurredDate\",
                   a.achievement_detail::text AS \"achievementDetailJson\",
                   a.certification_status AS \"certificationStatus\",
                   a.attachment_ref AS \"attachmentRef\",
                   a.created_at AS \"createdAt\",
                   a.updated_at AS \"updatedAt\"
            FROM lecture_evaluation_achievements a
            JOIN users teacher ON teacher.user_id = a.teacher_user_id
            LEFT JOIN korus_personnel_snapshots personnel ON personnel.employee_no = teacher.employee_no
            WHERE a.deleted_yn = 'N'
            <if test='criteria.managementNo != null and !criteria.managementNo.isBlank()'>
              AND a.management_no ILIKE CONCAT('%', #{criteria.managementNo}, '%')
            </if>
            <if test='criteria.teacherName != null and !criteria.teacherName.isBlank()'>
              AND personnel.name ILIKE CONCAT('%', #{criteria.teacherName}, '%')
            </if>
            <if test='criteria.managementItemCode != null and !criteria.managementItemCode.isBlank()'>
              AND a.management_item_code = #{criteria.managementItemCode}
            </if>
            <if test='criteria.occurredDateFrom != null'>
              AND a.occurred_date &gt;= #{criteria.occurredDateFrom}
            </if>
            <if test='criteria.occurredDateTo != null'>
              AND a.occurred_date &lt;= #{criteria.occurredDateTo}
            </if>
            <if test='criteria.certificationStatus != null and !criteria.certificationStatus.isBlank()'>
              AND a.certification_status = #{criteria.certificationStatus}
            </if>
            <if test='criteria.restrictToRequester'>
              AND a.teacher_user_id = #{criteria.requesterUserId}
            </if>
            ORDER BY a.occurred_date DESC, a.achievement_id DESC
            LIMIT #{criteria.safePageSize} OFFSET #{criteria.offset}
            </script>
            """)
    List<LectureEvaluationAchievementRow> list(@Param("criteria") LectureEvaluationAchievementSearchCriteria criteria);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM lecture_evaluation_achievements a
            JOIN users teacher ON teacher.user_id = a.teacher_user_id
            LEFT JOIN korus_personnel_snapshots personnel ON personnel.employee_no = teacher.employee_no
            WHERE a.deleted_yn = 'N'
            <if test='criteria.managementNo != null and !criteria.managementNo.isBlank()'>
              AND a.management_no ILIKE CONCAT('%', #{criteria.managementNo}, '%')
            </if>
            <if test='criteria.teacherName != null and !criteria.teacherName.isBlank()'>
              AND personnel.name ILIKE CONCAT('%', #{criteria.teacherName}, '%')
            </if>
            <if test='criteria.managementItemCode != null and !criteria.managementItemCode.isBlank()'>
              AND a.management_item_code = #{criteria.managementItemCode}
            </if>
            <if test='criteria.occurredDateFrom != null'>
              AND a.occurred_date &gt;= #{criteria.occurredDateFrom}
            </if>
            <if test='criteria.occurredDateTo != null'>
              AND a.occurred_date &lt;= #{criteria.occurredDateTo}
            </if>
            <if test='criteria.certificationStatus != null and !criteria.certificationStatus.isBlank()'>
              AND a.certification_status = #{criteria.certificationStatus}
            </if>
            <if test='criteria.restrictToRequester'>
              AND a.teacher_user_id = #{criteria.requesterUserId}
            </if>
            </script>
            """)
    long count(@Param("criteria") LectureEvaluationAchievementSearchCriteria criteria);

    @Select("""
            SELECT achievement_id AS \"achievementId\",
                   management_no AS \"managementNo\",
                   teacher_user_id AS \"teacherUserId\",
                   evaluation_year AS \"evaluationYear\",
                   organization_code AS \"organizationCode\",
                   management_item_code AS \"managementItemCode\",
                   occurred_date AS \"occurredDate\",
                   achievement_detail::text AS \"achievementDetailJson\",
                   certification_status AS \"certificationStatus\",
                   attachment_ref AS \"attachmentRef\",
                   created_at AS \"createdAt\",
                   updated_at AS \"updatedAt\"
            FROM lecture_evaluation_achievements
            WHERE achievement_id = #{achievementId}
              AND deleted_yn = 'N'
            """)
    LectureEvaluationAchievementRow findById(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM input_period_settings
                WHERE evaluation_year = #{evaluationYear}
                  AND active_yn = 'Y'
                  AND start_at &lt;= CURRENT_TIMESTAMP
                  AND end_at &gt;= CURRENT_TIMESTAMP
                  AND (area_code IS NULL OR area_code = 'EDUCATION')
            )
            """)
    boolean hasActiveInputPeriod(@Param("evaluationYear") String evaluationYear);

    @Select("""
            SELECT MIN(CAST(start_at AS date))
            FROM input_period_settings
            WHERE evaluation_year = #{evaluationYear}
              AND active_yn = 'Y'
              AND (area_code IS NULL OR area_code = 'EDUCATION')
            """)
    LocalDate findEvaluationPeriodStart(@Param("evaluationYear") String evaluationYear);

    @Select("""
            SELECT MAX(CAST(end_at AS date))
            FROM input_period_settings
            WHERE evaluation_year = #{evaluationYear}
              AND active_yn = 'Y'
              AND (area_code IS NULL OR area_code = 'EDUCATION')
            """)
    LocalDate findEvaluationPeriodEnd(@Param("evaluationYear") String evaluationYear);

    @Select("""
            SELECT a.achievement_id AS \"achievementId\",
                   a.management_no AS \"managementNo\",
                   a.teacher_user_id AS \"teacherUserId\",
                   personnel.name AS \"teacherName\",
                   a.evaluation_year AS \"evaluationYear\",
                   a.organization_code AS \"organizationCode\",
                   a.management_item_code AS \"managementItemCode\",
                   a.occurred_date AS \"occurredDate\",
                   a.achievement_detail::text AS \"achievementDetailJson\",
                   a.certification_status AS \"certificationStatus\",
                   a.attachment_ref AS \"attachmentRef\",
                   a.created_at AS \"createdAt\",
                   a.updated_at AS \"updatedAt\"
            FROM lecture_evaluation_achievements a
            JOIN users teacher ON teacher.user_id = a.teacher_user_id
            LEFT JOIN korus_personnel_snapshots personnel ON personnel.employee_no = teacher.employee_no
            WHERE a.management_no = #{managementNo}
              AND a.deleted_yn = 'N'
            """)
    LectureEvaluationAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    @Insert("""
            INSERT INTO lecture_evaluation_achievements (
                management_no, teacher_user_id, evaluation_year, organization_code,
                management_item_code, occurred_date, achievement_detail, attachment_ref,
                created_by, updated_by
            ) VALUES (
                #{command.managementNo}, #{command.teacherUserId}, #{command.evaluationYear},
                #{command.organizationCode}, #{command.managementItemCode}, #{command.occurredDate},
                CAST(#{command.achievementDetailJson} AS jsonb), #{command.attachmentRef},
                #{command.updatedBy}, #{command.updatedBy}
            )
            """)
    void insert(@Param("command") LectureEvaluationAchievementCommand command);

    @Update("""
            UPDATE lecture_evaluation_achievements
            SET management_item_code = #{command.managementItemCode},
                occurred_date = #{command.occurredDate},
                achievement_detail = CAST(#{command.achievementDetailJson} AS jsonb),
                attachment_ref = #{command.attachmentRef},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{command.updatedBy}
            WHERE achievement_id = #{command.achievementId}
              AND deleted_yn = 'N'
            """)
    int update(@Param("command") LectureEvaluationAchievementCommand command);

    @Update("""
            UPDATE lecture_evaluation_achievements
            SET certification_status = #{nextStatus},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{processedBy}
            WHERE achievement_id = #{achievementId}
              AND deleted_yn = 'N'
            """)
    int updateCertificationStatus(
            @Param("achievementId") Long achievementId,
            @Param("nextStatus") String nextStatus,
            @Param("processedBy") Long processedBy
    );

    @Insert("""
            INSERT INTO education_achievement_status_histories (
                achievement_type, achievement_id, previous_status, next_status, action_type,
                reason_code, opinion, processed_by, processed_at, created_by
            ) VALUES (
                #{history.achievementType}, #{history.achievementId}, #{history.previousStatus},
                #{history.nextStatus}, #{history.actionType}, #{history.reasonCode},
                #{history.opinion}, #{history.processedBy}, #{history.processedAt},
                #{history.processedBy}
            )
            """)
    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);

    @Insert("""
            INSERT INTO data_change_histories (
                target_business, target_key, change_type, field_name,
                before_value, after_value, changed_by, change_reason
            ) VALUES (
                #{targetBusiness}, #{targetKey}, #{changeType}, #{fieldName},
                #{beforeValue}, #{afterValue}, #{changedBy}, #{changeReason}
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
