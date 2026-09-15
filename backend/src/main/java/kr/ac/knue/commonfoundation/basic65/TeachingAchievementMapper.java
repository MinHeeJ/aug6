package kr.ac.knue.commonfoundation.basic65;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeachingAchievementMapper {
    List<TeachingAchievementRow> list(@Param("criteria") TeachingAchievementSearchCriteria criteria, @Param("facultyUserId") Long facultyUserId);
    long count(@Param("criteria") TeachingAchievementSearchCriteria criteria, @Param("facultyUserId") Long facultyUserId);
    TeachingAchievementRow findByIdAndFaculty(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId);
    TeachingAchievementRow findByUniqueKey(@Param("facultyUserId") Long facultyUserId, @Param("academicYear") String academicYear, @Param("semester") String semester, @Param("courseCode") String courseCode);
    void insert(@Param("request") SaveTeachingAchievementRequest request, @Param("facultyUserId") Long facultyUserId, @Param("dynamicFieldsJson") String dynamicFieldsJson, @Param("attachmentRefsJson") String attachmentRefsJson);
    void update(@Param("request") SaveTeachingAchievementRequest request, @Param("facultyUserId") Long facultyUserId, @Param("dynamicFieldsJson") String dynamicFieldsJson, @Param("attachmentRefsJson") String attachmentRefsJson);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("processedBy") Long processedBy, @Param("reason") String reason, @Param("requestId") String requestId);
}
