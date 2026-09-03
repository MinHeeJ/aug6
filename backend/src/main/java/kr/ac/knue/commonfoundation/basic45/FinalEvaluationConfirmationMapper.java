package kr.ac.knue.commonfoundation.basic45;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinalEvaluationConfirmationMapper {
    List<FinalEvaluationConfirmationTarget> listConfirmations(@Param("criteria") FinalEvaluationConfirmationSearchCriteria criteria);
    long countConfirmations(@Param("criteria") FinalEvaluationConfirmationSearchCriteria criteria);
    int countConfirmableMaterials(@Param("targetId") Long targetId, @Param("evaluationYear") String evaluationYear);
    int countCancelableMaterials(@Param("targetId") Long targetId, @Param("evaluationYear") String evaluationYear);
    long nextConfirmationSequence();
    int insertBatchRequest(@Param("batchId") String batchId,
                           @Param("jobType") String jobType,
                           @Param("targetConditionJson") String targetConditionJson,
                           @Param("requestUserId") Long requestUserId,
                           @Param("requestId") String requestId);
    int updateMaterialsStatus(@Param("targetId") Long targetId,
                              @Param("evaluationYear") String evaluationYear,
                              @Param("previousStatus") String previousStatus,
                              @Param("nextStatus") String nextStatus,
                              @Param("requestId") String requestId,
                              @Param("requestUserId") Long requestUserId);
    int insertMaterialStatusHistories(@Param("targetId") Long targetId,
                                      @Param("evaluationYear") String evaluationYear,
                                      @Param("previousStatus") String previousStatus,
                                      @Param("nextStatus") String nextStatus,
                                      @Param("changeReason") String changeReason,
                                      @Param("requestId") String requestId,
                                      @Param("requestUserId") Long requestUserId);
    int upsertConfirmation(@Param("targetId") Long targetId,
                           @Param("evaluationYear") String evaluationYear,
                           @Param("confirmationStatus") String confirmationStatus,
                           @Param("batchId") String batchId,
                           @Param("requestUserId") Long requestUserId,
                           @Param("requestId") String requestId);
    int markConfirmationCanceled(@Param("targetId") Long targetId,
                                 @Param("evaluationYear") String evaluationYear,
                                 @Param("batchId") String batchId,
                                 @Param("cancelReason") String cancelReason,
                                 @Param("requestUserId") Long requestUserId,
                                 @Param("requestId") String requestId);
    int insertBatchResult(@Param("batchId") String batchId,
                          @Param("jobType") String jobType,
                          @Param("totalCount") int totalCount,
                          @Param("successCount") int successCount,
                          @Param("failureCount") int failureCount,
                          @Param("excludedCount") int excludedCount,
                          @Param("requestId") String requestId);
}
