package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for lecture achievements and their required audit histories. */
@Mapper
public interface LectureAchievementMapper {
    List<LectureAchievementRow> list(@Param("criteria") LectureAchievementSearchCriteria criteria);

    long count(@Param("criteria") LectureAchievementSearchCriteria criteria);

    LectureAchievementRow findById(@Param("achievementId") Long achievementId);

    LectureAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    String findActiveOrganizationCode(@Param("targetUserId") Long targetUserId);

    int insertAchievement(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy,
            @Param("changeReason") String changeReason
    );

    int updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason
    );

    int insertChangeHistory(
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason
    );

    int updateCertificationStatus(
            @Param("achievementId") Long achievementId,
            @Param("nextStatus") String nextStatus,
            @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason
    );

    int insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus,
            @Param("actionType") String actionType,
            @Param("reasonCode") String reasonCode,
            @Param("opinion") String opinion,
            @Param("processedBy") Long processedBy,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId
    );
}
