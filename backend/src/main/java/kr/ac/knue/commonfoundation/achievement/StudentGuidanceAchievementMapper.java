package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Persists FR-027 headers and JSONB student details without exposing personal data as columns. */
@Mapper
public interface StudentGuidanceAchievementMapper {
    @Select("SELECT organization_code FROM korus_personnel_snapshots WHERE employee_no = (SELECT employee_no FROM users WHERE user_id = #{userId}) ORDER BY updated_at DESC LIMIT 1")
    String findOrganizationCodeForUser(@Param("userId") Long userId);

    @Select("SELECT achievement_id AS \"achievementId\", evaluation_year AS \"evaluationYear\", teacher_user_id AS \"teacherUserId\", organization_code AS \"organizationCode\", management_item_code AS \"managementItemCode\", guidance_start_date AS \"guidanceStartDate\", guidance_end_date AS \"guidanceEndDate\", student_count AS \"studentCount\", attachment_ref AS \"attachmentRef\", certification_status AS \"certificationStatus\" FROM student_guidance_achievements WHERE achievement_id = #{achievementId} AND deleted_yn = 'N'")
    StudentGuidanceRow findById(@Param("achievementId") Long achievementId);

    @Insert("INSERT INTO student_guidance_achievements (evaluation_year, teacher_user_id, organization_code, management_item_code, guidance_start_date, guidance_end_date, student_count, attachment_ref, created_by, updated_by) VALUES (#{evaluationYear}, #{teacherUserId}, #{organizationCode}, #{request.managementItemCode}, #{request.guidanceStartDate}, #{request.guidanceEndDate}, #{studentCount}, #{request.attachmentRef}, #{actorUserId}, #{actorUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "request.achievementId", keyColumn = "achievement_id")
    void insert(@Param("request") StudentGuidanceSaveRequest request, @Param("evaluationYear") String evaluationYear, @Param("teacherUserId") Long teacherUserId, @Param("organizationCode") String organizationCode, @Param("studentCount") int studentCount, @Param("actorUserId") Long actorUserId);

    @Update("UPDATE student_guidance_achievements SET management_item_code=#{request.managementItemCode}, guidance_start_date=#{request.guidanceStartDate}, guidance_end_date=#{request.guidanceEndDate}, student_count=#{studentCount}, attachment_ref=#{request.attachmentRef}, updated_at=CURRENT_TIMESTAMP, updated_by=#{actorUserId} WHERE achievement_id=#{request.achievementId} AND deleted_yn='N'")
    void update(@Param("request") StudentGuidanceSaveRequest request, @Param("studentCount") int studentCount, @Param("actorUserId") Long actorUserId);

    @Delete("DELETE FROM student_guidance_students WHERE achievement_id = #{achievementId}")
    void deleteStudents(@Param("achievementId") Long achievementId);

    @Insert("INSERT INTO student_guidance_students (achievement_id, student_detail_jsonb, created_by, updated_by) VALUES (#{achievementId}, CAST(#{detail} AS jsonb), #{actorUserId}, #{actorUserId})")
    void insertStudent(@Param("achievementId") Long achievementId, @Param("detail") String detail, @Param("actorUserId") Long actorUserId);

    @Insert("INSERT INTO data_change_histories (target_business, target_key, change_type, field_name, after_value, changed_by, change_reason) VALUES ('student_guidance_achievements', #{achievementId}::text, #{changeType}, 'student_detail_jsonb', #{afterValue}, #{actorUserId}, #{reason})")
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType, @Param("afterValue") String afterValue, @Param("actorUserId") Long actorUserId, @Param("reason") String reason);

    record StudentGuidanceRow(Long achievementId, String evaluationYear, Long teacherUserId, String organizationCode, String managementItemCode, LocalDate guidanceStartDate, LocalDate guidanceEndDate, int studentCount, String attachmentRef, String certificationStatus) {}
}
