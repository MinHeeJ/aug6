package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis persistence boundary for the BASIC-83 course operation source and
 * detail tables. SQL scopes data through the service before mutation.
 */
@Mapper
public interface CourseOperationMapper {
    @Select("""
            SELECT
                achievement.achievement_id,
                achievement.teacher_user_id,
                owner.login_id AS teacher_name,
                achievement.management_item_code,
                achievement.achievement_date,
                detail.performance_detail AS performance_details,
                achievement.achievement_status,
                achievement.attachment_ids,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            JOIN course_operation_achievement_details detail
                ON detail.achievement_id = achievement.achievement_id
            WHERE achievement.achievement_id = #{achievementId}
              AND achievement.achievement_type = 'COURSE_OPERATION'
              AND achievement.deleted_yn = 'N'
            """)
    @ConstructorArgs({
            @Arg(column = "achievement_id", javaType = Long.class),
            @Arg(column = "teacher_user_id", javaType = Long.class),
            @Arg(column = "teacher_name", javaType = String.class),
            @Arg(column = "management_item_code", javaType = String.class),
            @Arg(column = "achievement_date", javaType = LocalDate.class),
            @Arg(column = "performance_details", javaType = String.class),
            @Arg(column = "achievement_status", javaType = String.class),
            @Arg(
                    column = "attachment_ids",
                    javaType = String[].class,
                    typeHandler = org.apache.ibatis.type.ArrayTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.LocalDateTime.class),
            @Arg(column = "updated_at", javaType = java.time.LocalDateTime.class)
    })
    @Results(id = "courseOperationRow", value = {
            @Result(column = "achievement_id", property = "achievementId"),
            @Result(column = "teacher_user_id", property = "teacherUserId"),
            @Result(column = "teacher_name", property = "teacherName"),
            @Result(column = "management_item_code", property = "managementItemCode"),
            @Result(column = "achievement_date", property = "achievementDate"),
            @Result(column = "performance_details", property = "performanceDetails"),
            @Result(column = "achievement_status", property = "achievementStatus"),
            @Result(
                    column = "attachment_ids",
                    property = "attachmentIds",
                    javaType = String[].class,
                    typeHandler = org.apache.ibatis.type.ArrayTypeHandler.class),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    CourseOperationRow findById(@Param("achievementId") Long achievementId);

    @Select("""
            SELECT
                achievement.achievement_id,
                achievement.teacher_user_id,
                owner.login_id AS teacher_name,
                achievement.management_item_code,
                achievement.achievement_date,
                detail.performance_detail AS performance_details,
                achievement.achievement_status,
                achievement.attachment_ids,
                achievement.created_at,
                achievement.updated_at
            FROM education_achievements achievement
            JOIN users owner
                ON owner.user_id = achievement.teacher_user_id
            JOIN course_operation_achievement_details detail
                ON detail.achievement_id = achievement.achievement_id
            WHERE achievement.achievement_type = 'COURSE_OPERATION'
              AND achievement.deleted_yn = 'N'
              AND (
                    #{selfOnly} = FALSE
                    OR achievement.teacher_user_id = #{requesterUserId}
              )
            ORDER BY achievement.achievement_date DESC, achievement.achievement_id DESC
            LIMIT #{pageSize}
            OFFSET #{offset}
            """)
    @ConstructorArgs({
            @Arg(column = "achievement_id", javaType = Long.class),
            @Arg(column = "teacher_user_id", javaType = Long.class),
            @Arg(column = "teacher_name", javaType = String.class),
            @Arg(column = "management_item_code", javaType = String.class),
            @Arg(column = "achievement_date", javaType = LocalDate.class),
            @Arg(column = "performance_details", javaType = String.class),
            @Arg(column = "achievement_status", javaType = String.class),
            @Arg(
                    column = "attachment_ids",
                    javaType = String[].class,
                    typeHandler = org.apache.ibatis.type.ArrayTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.LocalDateTime.class),
            @Arg(column = "updated_at", javaType = java.time.LocalDateTime.class)
    })
    @Results(value = {
            @Result(column = "achievement_id", property = "achievementId"),
            @Result(column = "teacher_user_id", property = "teacherUserId"),
            @Result(column = "teacher_name", property = "teacherName"),
            @Result(column = "management_item_code", property = "managementItemCode"),
            @Result(column = "achievement_date", property = "achievementDate"),
            @Result(column = "performance_details", property = "performanceDetails"),
            @Result(column = "achievement_status", property = "achievementStatus"),
            @Result(
                    column = "attachment_ids",
                    property = "attachmentIds",
                    javaType = String[].class,
                    typeHandler = org.apache.ibatis.type.ArrayTypeHandler.class),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    List<CourseOperationRow> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("selfOnly") boolean selfOnly,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*)
            FROM education_achievements achievement
            WHERE achievement.achievement_type = 'COURSE_OPERATION'
              AND achievement.deleted_yn = 'N'
              AND (
                    #{selfOnly} = FALSE
                    OR achievement.teacher_user_id = #{requesterUserId}
              )
            """)
    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("selfOnly") boolean selfOnly);

    @Insert("""
            INSERT INTO education_achievements (
                achievement_type,
                teacher_user_id,
                management_item_code,
                achievement_date,
                achievement_status,
                attachment_ids,
                deleted_yn,
                created_by,
                updated_by
            )
            VALUES (
                'COURSE_OPERATION',
                #{teacherUserId},
                #{managementItemCode},
                #{achievementDate},
                'DRAFT',
                #{attachmentIds, typeHandler=org.apache.ibatis.type.ArrayTypeHandler},
                'N',
                #{actorUserId},
                #{actorUserId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "achievementId")
    void insertAchievement(CourseOperationCommand command);

    @Insert("""
            INSERT INTO course_operation_achievement_details (
                achievement_id,
                performance_detail
            )
            VALUES (
                #{achievementId},
                #{performanceDetails}
            )
            """)
    void insertDetails(CourseOperationCommand command);

    @Update("""
            UPDATE education_achievements
            SET management_item_code = #{managementItemCode},
                achievement_date = #{achievementDate},
                attachment_ids = #{attachmentIds, typeHandler=org.apache.ibatis.type.ArrayTypeHandler},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{actorUserId}
            WHERE achievement_id = #{achievementId}
              AND achievement_type = 'COURSE_OPERATION'
              AND deleted_yn = 'N'
            """)
    void updateAchievement(CourseOperationCommand command);

    @Update("""
            UPDATE course_operation_achievement_details
            SET performance_detail = #{performanceDetails}
            WHERE achievement_id = #{achievementId}
            """)
    void updateDetails(CourseOperationCommand command);

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
                'course_operation_achievement_details',
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
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("actorUserId") Long actorUserId,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
