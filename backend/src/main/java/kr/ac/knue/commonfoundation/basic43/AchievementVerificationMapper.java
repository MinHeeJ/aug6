package kr.ac.knue.commonfoundation.basic43;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AchievementVerificationMapper {
    List<AchievementVerificationRow> listAchievementVerificationTargets(@Param("criteria") AchievementVerificationSearchCriteria criteria);
    long countAchievementVerificationTargets(@Param("criteria") AchievementVerificationSearchCriteria criteria);
    AchievementVerificationRow findLatestByAchievementId(@Param("achievementId") Long achievementId);
    int handlerScopeExists(@Param("achievementId") Long achievementId, @Param("handlerUserId") Long handlerUserId);
    int transitionAllowed(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
    int rejectionReasonExists(@Param("reasonCode") String reasonCode);
    AchievementVerificationRow insertTransition(@Param("achievementId") Long achievementId,
                                                @Param("evaluationYear") String evaluationYear,
                                                @Param("handlerUserId") Long handlerUserId,
                                                @Param("actionType") String actionType,
                                                @Param("previousStatus") String previousStatus,
                                                @Param("nextStatus") String nextStatus,
                                                @Param("opinion") String opinion,
                                                @Param("evidenceRef") String evidenceRef,
                                                @Param("reasonCode") String reasonCode,
                                                @Param("processedBy") Long processedBy,
                                                @Param("changeReason") String changeReason);
}
