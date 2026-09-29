package kr.ac.knue.commonfoundation.lectureevaluations;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for lecture-evaluation achievement data and audit records. */
@Mapper
public interface LectureEvaluationAchievementMapper {
    List<LectureEvaluationAchievementRow> list(@Param("criteria") LectureEvaluationAchievementSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long count(@Param("criteria") LectureEvaluationAchievementSearchCriteria criteria);
    LectureEvaluationAchievementRow find(@Param("achievementId") Long achievementId);
    int insert(@Param("request") LectureEvaluationAchievementSaveRequest request, @Param("evaluationYear") String evaluationYear,
            @Param("userId") Long userId);
    int update(@Param("request") LectureEvaluationAchievementSaveRequest request, @Param("userId") Long userId);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus, @Param("actionType") String actionType,
            @Param("reason") String reason, @Param("userId") Long userId);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("userId") Long userId, @Param("reason") String reason);
}
