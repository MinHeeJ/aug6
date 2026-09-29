package kr.ac.knue.commonfoundation.degreecompletions;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for degree-completion headers, student details, and audit evidence. */
@Mapper
public interface DegreeCompletionAchievementMapper {
    List<DegreeCompletionAchievementRow> list(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long count(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria);
    DegreeCompletionAchievementRow find(@Param("achievementId") Long achievementId);
    int insert(@Param("request") DegreeCompletionAchievementSaveRequest request, @Param("evaluationYear") String evaluationYear,
            @Param("userId") Long userId);
    int update(@Param("request") DegreeCompletionAchievementSaveRequest request, @Param("userId") Long userId);
    int deleteStudents(@Param("achievementId") Long achievementId);
    int insertStudent(@Param("achievementId") Long achievementId, @Param("student") DegreeCompletionStudentRequest student);
    List<DegreeCompletionStudentRequest> findStudents(@Param("achievementId") Long achievementId);
    int insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("userId") Long userId, @Param("reason") String reason);
}
