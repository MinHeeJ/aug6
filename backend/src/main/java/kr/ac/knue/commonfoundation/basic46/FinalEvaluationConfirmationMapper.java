package kr.ac.knue.commonfoundation.basic46;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinalEvaluationConfirmationMapper {
    List<FinalEvaluationConfirmationRow> listFinalEvaluationConfirmations(
            @Param("criteria") FinalEvaluationConfirmationSearchCriteria criteria);

    long countFinalEvaluationConfirmations(@Param("criteria") FinalEvaluationConfirmationSearchCriteria criteria);

    String findLatestEvaluationYearForTarget(@Param("targetUserId") Long targetUserId);

    FinalEvaluationConfirmationRow findConfirmationCandidate(@Param("targetUserId") Long targetUserId,
                                                             @Param("evaluationYear") String evaluationYear);

    void insertFinalizationBatchJob(@Param("batchJobId") String batchJobId,
                                    @Param("batchType") String batchType,
                                    @Param("targetUserId") Long targetUserId,
                                    @Param("evaluationYear") String evaluationYear,
                                    @Param("totalCount") int totalCount,
                                    @Param("successCount") int successCount,
                                    @Param("failureCount") int failureCount,
                                    @Param("excludedCount") int excludedCount,
                                    @Param("requestedBy") Long requestedBy,
                                    @Param("requestId") String requestId,
                                    @Param("reason") String reason);

    void insertBatchJobItem(@Param("batchJobId") String batchJobId,
                            @Param("targetRef") String targetRef,
                            @Param("resultStatus") String resultStatus,
                            @Param("errorCode") String errorCode,
                            @Param("errorMessage") String errorMessage,
                            @Param("excludedReason") String excludedReason,
                            @Param("requestId") String requestId,
                            @Param("createdBy") Long createdBy);

    int updateMaterialsStatus(@Param("targetUserId") Long targetUserId,
                              @Param("evaluationYear") String evaluationYear,
                              @Param("fromStatus") String fromStatus,
                              @Param("toStatus") String toStatus,
                              @Param("requestId") String requestId,
                              @Param("updatedBy") Long updatedBy);

    void insertFinalization(@Param("targetUserId") Long targetUserId,
                            @Param("evaluationYear") String evaluationYear,
                            @Param("finalStatus") String finalStatus,
                            @Param("confirmedBy") Long confirmedBy,
                            @Param("canceledBy") Long canceledBy,
                            @Param("cancelReason") String cancelReason,
                            @Param("snapshotRef") String snapshotRef,
                            @Param("snapshotJson") String snapshotJson,
                            @Param("batchJobId") String batchJobId,
                            @Param("requestId") String requestId,
                            @Param("createdBy") Long createdBy);

    default void updateMaterialScoresDirectly(Long targetUserId, Object ignored) {
        throw new UnsupportedOperationException("Final evaluation confirmation must not directly change scores.");
    }
}
