package kr.ac.knue.commonfoundation.basic65;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GraduateAchievementMapper {
    List<GraduateAchievementRow> list(@Param("criteria") GraduateAchievementSearchCriteria criteria, @Param("facultyUserId") Long facultyUserId);
    long count(@Param("criteria") GraduateAchievementSearchCriteria criteria, @Param("facultyUserId") Long facultyUserId);
    GraduateAchievementRow findByIdAndFaculty(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId);
    GraduateAchievementRow findDuplicate(@Param("facultyUserId") Long facultyUserId, @Param("studentNo") String studentNo, @Param("degreeType") String degreeType, @Param("awardDate") String awardDate);
    void insert(@Param("request") SaveGraduateAchievementRequest request, @Param("facultyUserId") Long facultyUserId, @Param("dynamicFieldsJson") String dynamicFieldsJson, @Param("attachmentRefsJson") String attachmentRefsJson);
    void update(@Param("request") SaveGraduateAchievementRequest request, @Param("facultyUserId") Long facultyUserId, @Param("dynamicFieldsJson") String dynamicFieldsJson, @Param("attachmentRefsJson") String attachmentRefsJson);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId, @Param("reason") String reason, @Param("requestId") String requestId);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("facultyUserId") Long facultyUserId, @Param("reason") String reason, @Param("requestId") String requestId);
}
