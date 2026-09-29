package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for degree-completion headers, students, and audit writes. */
@Mapper
public interface DegreeCompletionAchievementMapper {
    List<DegreeCompletionAchievementRow> list(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria);
    long count(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria);
    void insertAchievement(@Param("managementNo") String managementNo, @Param("evaluationYear") String evaluationYear,
            @Param("teacherUserId") Long teacherUserId, @Param("teacherName") String teacherName,
            @Param("managementItemCode") String managementItemCode, @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail, @Param("userId") Long userId);
    void insertStudent(@Param("achievementId") Long achievementId, @Param("student") SaveDegreeCompletionAchievementRequest.StudentInput student, @Param("userId") Long userId);
    DegreeCompletionAchievementRow findByManagementNo(@Param("managementNo") String managementNo);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("userId") Long userId);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("userId") Long userId, @Param("requestId") String requestId);
}
