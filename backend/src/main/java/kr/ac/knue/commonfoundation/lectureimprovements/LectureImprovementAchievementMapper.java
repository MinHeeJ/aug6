package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.CreateCommand;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis boundary for lecture-improvement headers, details, and required
 * lifecycle audit rows backed by the BASIC-83 foundation schema.
 */
@Mapper
public interface LectureImprovementAchievementMapper {
    List<Row> list(
            @Param("criteria") SearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") SearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    Row findScoped(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    Row findById(@Param("achievementId") Long achievementId);

    String findOrganizationCode(@Param("userId") Long userId);

    void insertAchievement(@Param("command") CreateCommand command);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester);

    void updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") java.time.LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester);

    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("previousStatus") EducationAchievementStatus previousStatus,
            @Param("nextStatus") EducationAchievementStatus nextStatus,
            @Param("actionType") String actionType,
            @Param("opinion") String opinion,
            @Param("processedBy") Long processedBy,
            @Param("processedAt") LocalDateTime processedAt);

    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
