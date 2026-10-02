package kr.ac.knue.commonfoundation.f4_user_story_3;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Row;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence adapter for lecture-improvement records. Every read uses
 * the same requester role/data-scope predicate to prevent detail-read bypasses.
 */
@Mapper
public interface LectureImprovementMapper {
    @Select("""
            <script>
            SELECT achievement.achievement_id AS \"achievementId\",
                   achievement.management_no AS \"managementNo\",
                   achievement.target_user_id AS \"targetUserId\",
                   achievement.management_item_code AS \"managementItemCode\",
                   achievement.occurred_date AS \"achievementDate\",
                   achievement.performance_content AS \"achievementContent\",
                   achievement.academic_year AS \"academicYear\",
                   achievement.semester_code AS \"semester\",
                   achievement.certification_status AS \"certificationStatus\",
                   achievement.attachment_ids::text AS \"attachmentIds\",
                   achievement.created_at AS \"createdAt\",
                   achievement.updated_at AS \"updatedAt\"
            FROM teaching_improvement_achievements achievement
            WHERE achievement.deleted_yn = 'N'
              AND <choose>
                    <when test="roles.contains('R01')">
                        achievement.target_user_id = #{requesterUserId}
                    </when>
                    <when test="roles.contains('R02')">
                        EXISTS (
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
                        EXISTS (
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
                    <otherwise>1 = 0</otherwise>
                  </choose>
            ORDER BY achievement.occurred_date DESC, achievement.achievement_id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "achievementId", javaType = Long.class, id = true),
        @Arg(column = "managementNo", javaType = String.class),
        @Arg(column = "targetUserId", javaType = Long.class),
        @Arg(column = "managementItemCode", javaType = String.class),
        @Arg(column = "achievementDate", javaType = LocalDate.class),
        @Arg(column = "achievementContent", javaType = String.class),
        @Arg(column = "academicYear", javaType = Integer.class),
        @Arg(column = "semester", javaType = Integer.class),
        @Arg(column = "certificationStatus", javaType = String.class),
        @Arg(column = "attachmentIds", javaType = String.class),
        @Arg(column = "createdAt", javaType = java.time.LocalDateTime.class),
        @Arg(column = "updatedAt", javaType = java.time.LocalDateTime.class)
    })
    List<Row> list(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM teaching_improvement_achievements achievement
            WHERE achievement.deleted_yn = 'N'
              AND <choose>
                    <when test="roles.contains('R01')">
                        achievement.target_user_id = #{requesterUserId}
                    </when>
                    <when test="roles.contains('R02')">
                        EXISTS (
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
                        EXISTS (
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
                    <otherwise>1 = 0</otherwise>
                  </choose>
            </script>
            """)
    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            <script>
            SELECT achievement.achievement_id AS \"achievementId\",
                   achievement.management_no AS \"managementNo\",
                   achievement.target_user_id AS \"targetUserId\",
                   achievement.management_item_code AS \"managementItemCode\",
                   achievement.occurred_date AS \"achievementDate\",
                   achievement.performance_content AS \"achievementContent\",
                   achievement.academic_year AS \"academicYear\",
                   achievement.semester_code AS \"semester\",
                   achievement.certification_status AS \"certificationStatus\",
                   achievement.attachment_ids::text AS \"attachmentIds\",
                   achievement.created_at AS \"createdAt\",
                   achievement.updated_at AS \"updatedAt\"
            FROM teaching_improvement_achievements achievement
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.deleted_yn = 'N'
              AND <choose>
                    <when test="roles.contains('R01')">
                        achievement.target_user_id = #{requesterUserId}
                    </when>
                    <when test="roles.contains('R02')">
                        EXISTS (
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
                        EXISTS (
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
                    <otherwise>1 = 0</otherwise>
                  </choose>
            </script>
            """)
    @ConstructorArgs({
        @Arg(column = "achievementId", javaType = Long.class, id = true),
        @Arg(column = "managementNo", javaType = String.class),
        @Arg(column = "targetUserId", javaType = Long.class),
        @Arg(column = "managementItemCode", javaType = String.class),
        @Arg(column = "achievementDate", javaType = LocalDate.class),
        @Arg(column = "achievementContent", javaType = String.class),
        @Arg(column = "academicYear", javaType = Integer.class),
        @Arg(column = "semester", javaType = Integer.class),
        @Arg(column = "certificationStatus", javaType = String.class),
        @Arg(column = "attachmentIds", javaType = String.class),
        @Arg(column = "createdAt", javaType = java.time.LocalDateTime.class),
        @Arg(column = "updatedAt", javaType = java.time.LocalDateTime.class)
    })
    Row findById(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    @Select("""
            SELECT achievement.achievement_id AS \"achievementId\",
                   achievement.management_no AS \"managementNo\",
                   achievement.target_user_id AS \"targetUserId\",
                   achievement.management_item_code AS \"managementItemCode\",
                   achievement.occurred_date AS \"achievementDate\",
                   achievement.performance_content AS \"achievementContent\",
                   achievement.academic_year AS \"academicYear\",
                   achievement.semester_code AS \"semester\",
                   achievement.certification_status AS \"certificationStatus\",
                   achievement.attachment_ids::text AS \"attachmentIds\",
                   achievement.created_at AS \"createdAt\",
                   achievement.updated_at AS \"updatedAt\"
            FROM teaching_improvement_achievements achievement
            WHERE achievement.management_no = #{managementNo}
              AND achievement.deleted_yn = 'N'
              AND achievement.target_user_id = #{requesterUserId}
            """)
    @ConstructorArgs({
        @Arg(column = "achievementId", javaType = Long.class, id = true),
        @Arg(column = "managementNo", javaType = String.class),
        @Arg(column = "targetUserId", javaType = Long.class),
        @Arg(column = "managementItemCode", javaType = String.class),
        @Arg(column = "achievementDate", javaType = LocalDate.class),
        @Arg(column = "achievementContent", javaType = String.class),
        @Arg(column = "academicYear", javaType = Integer.class),
        @Arg(column = "semester", javaType = Integer.class),
        @Arg(column = "certificationStatus", javaType = String.class),
        @Arg(column = "attachmentIds", javaType = String.class),
        @Arg(column = "createdAt", javaType = java.time.LocalDateTime.class),
        @Arg(column = "updatedAt", javaType = java.time.LocalDateTime.class)
    })
    Row findByManagementNo(
            @Param("managementNo") String managementNo,
            @Param("requesterUserId") Long requesterUserId);

    @Insert("""
            INSERT INTO teaching_improvement_achievements (
                management_no,
                target_user_id,
                evaluation_year,
                management_item_code,
                occurred_date,
                performance_content,
                academic_year,
                semester_code,
                attachment_ids,
                created_by,
                updated_by
            ) VALUES (
                #{managementNo},
                #{targetUserId},
                #{evaluationYear},
                #{managementItemCode},
                #{achievementDate},
                #{achievementContent},
                #{academicYear},
                #{semester},
                CAST(#{attachmentIds} AS jsonb),
                #{createdBy},
                #{createdBy}
            )
            """)
    void insert(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);

    @Update("""
            UPDATE teaching_improvement_achievements
            SET management_item_code = #{managementItemCode},
                occurred_date = #{achievementDate},
                performance_content = #{achievementContent},
                academic_year = #{academicYear},
                semester_code = #{semester},
                attachment_ids = CAST(#{attachmentIds} AS jsonb),
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{updatedBy}
            WHERE achievement_id = #{achievementId}
              AND target_user_id = #{requesterUserId}
              AND certification_status != 'EVALUATION_CONFIRMED'
              AND deleted_yn = 'N'
            """)
    int update(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy,
            @Param("requesterUserId") Long requesterUserId);

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
                'teaching_improvement_achievements',
                #{achievementId},
                #{changeType},
                'performance_content',
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
