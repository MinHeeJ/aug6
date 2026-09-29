package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for lecture rows and their required audit writes. */
@Mapper
public interface LectureAchievementMapper {
    List<LectureAchievementRow> list(@Param("criteria") LectureAchievementSearchCriteria criteria);

    long count(@Param("criteria") LectureAchievementSearchCriteria criteria);

    void insertAchievement(@Param("managementNo") String managementNo,
            @Param("evaluationYear") String evaluationYear,
            @Param("teacherUserId") Long teacherUserId,
            @Param("teacherName") String teacherName,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") java.time.LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("userId") Long userId);

    LectureAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    LectureAchievementRow findByIdForUpdate(@Param("achievementId") Long achievementId);

    void updateCertificationStatus(@Param("achievementId") Long achievementId,
            @Param("certificationStatus") String certificationStatus,
            @Param("userId") Long userId);

    void insertChangeHistory(@Param("achievementId") Long achievementId,
            @Param("userId") Long userId,
            @Param("requestId") String requestId);

    void insertStatusHistory(@Param("achievementId") Long achievementId,
            @Param("userId") Long userId);

    void insertTransitionStatusHistory(@Param("achievementId") Long achievementId,
            @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus,
            @Param("actionType") String actionType,
            @Param("reasonCode") String reasonCode,
            @Param("opinion") String opinion,
            @Param("userId") Long userId);
}
