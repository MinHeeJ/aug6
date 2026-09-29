package kr.ac.knue.commonfoundation.lectureachievements;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for lecture-achievement data and audit records. */
@Mapper
public interface LectureAchievementMapper {
    List<LectureAchievementRow> list(@Param("criteria") LectureAchievementSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long count(@Param("criteria") LectureAchievementSearchCriteria criteria);
    LectureAchievementRow find(@Param("achievementId") Long achievementId);
    int insert(@Param("request") LectureAchievementSaveRequest request, @Param("evaluationYear") String evaluationYear,
            @Param("userId") Long userId);
    int update(@Param("request") LectureAchievementSaveRequest request, @Param("userId") Long userId);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus, @Param("actionType") String actionType,
            @Param("reason") String reason, @Param("userId") Long userId);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("userId") Long userId, @Param("reason") String reason);
}
