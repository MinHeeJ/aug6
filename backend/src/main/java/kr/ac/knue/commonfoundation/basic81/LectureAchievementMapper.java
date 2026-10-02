package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for lecture rows and their mandatory lifecycle/audit records. */
@Mapper
public interface LectureAchievementMapper {
    List<LectureAchievementRow> listLectureAchievements(
            @Param("criteria") LectureAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);
    long countLectureAchievements(
            @Param("criteria") LectureAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);
    LectureAchievementRow findLectureAchievement(@Param("achievementId") Long achievementId);
    LectureAchievementRow findLectureAchievementByManagementNo(@Param("managementNo") String managementNo);
    void insertLectureAchievement(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy);
    void updateLectureAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy);
    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);
    void insertChangeHistory(
            @Param("targetBusiness") String targetBusiness,
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
