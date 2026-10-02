package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Persistence boundary for lecture-evaluation source rows and their mandatory
 * status and data-change audit records.
 */
@Mapper
public interface LectureEvaluationAchievementMapper {
    List<LectureEvaluationAchievementRow> listLectureEvaluationAchievements(
            @Param("criteria") LectureEvaluationAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long countLectureEvaluationAchievements(
            @Param("criteria") LectureEvaluationAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    void insertLectureEvaluationAchievement(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") java.time.LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy);

    LectureEvaluationAchievementRow findLectureEvaluationAchievement(
            @Param("managementNo") String managementNo);

    void insertStatusHistory(
            @Param("history") EducationAchievementStatusHistory history);

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

    void updateCertificationStatus(
            @Param("achievementId") Long achievementId,
            @Param("status") String status,
            @Param("updatedBy") Long updatedBy);

    String findCertificationStatus(@Param("achievementId") Long achievementId);
}
