package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Data-access boundary for BASIC-79 shared permission, period, lock, and status-history checks.
 */
@Mapper
public interface LectureAchievementFoundationMapper {
    int countAllowedFunctionPermission(@Param("screenId") String screenId, @Param("roleCode") String roleCode,
                                       @Param("functionType") String functionType);

    int countActiveInputPeriod(@Param("evaluationYear") String evaluationYear,
                               @Param("organizationCode") String organizationCode);

    int countActiveManagementItem(@Param("evaluationYear") String evaluationYear,
                                  @Param("managementItemCode") String managementItemCode,
                                  @Param("occurredDate") LocalDate occurredDate);

    int countEvaluationConfirmedFinalization(@Param("ownerUserId") Long ownerUserId,
                                             @Param("evaluationYear") String evaluationYear);

    LectureEvaluationAchievementState findLectureForUpdate(@Param("achievementId") Long achievementId);

    int countLectureScope(@Param("achievementId") Long achievementId, @Param("userId") Long userId);

    int updateLectureStatus(@Param("achievementId") Long achievementId,
                                      @Param("previousStatus") String previousStatus,
                                      @Param("nextStatus") String nextStatus,
                                      @Param("updatedBy") Long updatedBy);

    int insertStatusHistory(@Param("achievementType") String achievementType, @Param("achievementId") Long achievementId,
                            @Param("previousStatus") String previousStatus, @Param("nextStatus") String nextStatus,
                            @Param("actionType") String actionType, @Param("reasonCode") String reasonCode,
                            @Param("opinion") String opinion, @Param("processedBy") Long processedBy);

    int insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey,
                            @Param("changeType") String changeType, @Param("fieldName") String fieldName,
                            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
                            @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);
}
