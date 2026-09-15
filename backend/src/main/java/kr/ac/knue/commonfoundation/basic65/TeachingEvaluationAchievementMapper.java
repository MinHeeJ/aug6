package kr.ac.knue.commonfoundation.basic65;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeachingEvaluationAchievementMapper {
    List<TeachingEvaluationAchievementRow> list(
            @Param("criteria") TeachingEvaluationAchievementSearchCriteria criteria,
            @Param("facultyUserId") Long facultyUserId);

    long count(
            @Param("criteria") TeachingEvaluationAchievementSearchCriteria criteria,
            @Param("facultyUserId") Long facultyUserId);

    TeachingEvaluationAchievementRow findByUniqueKey(
            @Param("facultyUserId") Long facultyUserId,
            @Param("academicYear") String academicYear,
            @Param("semester") String semester,
            @Param("courseCode") String courseCode);

    void insert(
            @Param("request") SaveTeachingEvaluationAchievementRequest request,
            @Param("facultyUserId") Long facultyUserId,
            @Param("dynamicFieldsJson") String dynamicFieldsJson,
            @Param("attachmentRefsJson") String attachmentRefsJson);

    TeachingEvaluationAchievementRow findById(@Param("achievementId") Long achievementId);

    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("processedBy") Long processedBy,
            @Param("reason") String reason,
            @Param("requestId") String requestId);
}
