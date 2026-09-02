package kr.ac.knue.commonfoundation.basic43;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GrantPaymentApprovalMapper {
    List<GrantPaymentApprovalRow> listGrantPaymentApprovals(@Param("criteria") GrantPaymentApprovalSearchCriteria criteria);
    long countGrantPaymentApprovals(@Param("criteria") GrantPaymentApprovalSearchCriteria criteria);
    GrantPaymentApprovalRow findLatestByGrantApplicationId(@Param("grantApplicationId") Long grantApplicationId);
    int paymentScopeExists(@Param("grantApplicationId") Long grantApplicationId, @Param("handlerUserId") Long handlerUserId);
    int transitionAllowed(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
    int rejectionReasonExists(@Param("reasonCode") String reasonCode);
    GrantPaymentApprovalRow insertTransition(@Param("grantApplicationId") Long grantApplicationId,
                                             @Param("linkedAchievementId") Long linkedAchievementId,
                                             @Param("evaluationYear") String evaluationYear,
                                             @Param("approvalStatus") String approvalStatus,
                                             @Param("previousStatus") String previousStatus,
                                             @Param("nextStatus") String nextStatus,
                                             @Param("requestedAmountSnapshot") BigDecimal requestedAmountSnapshot,
                                             @Param("paymentAmountSnapshot") BigDecimal paymentAmountSnapshot,
                                             @Param("accountSnapshotRef") String accountSnapshotRef,
                                             @Param("reasonCode") String reasonCode,
                                             @Param("opinion") String opinion,
                                             @Param("processedBy") Long processedBy,
                                             @Param("changeReason") String changeReason);
}
