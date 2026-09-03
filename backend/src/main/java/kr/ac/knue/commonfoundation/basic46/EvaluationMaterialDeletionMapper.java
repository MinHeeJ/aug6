package kr.ac.knue.commonfoundation.basic46;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationMaterialDeletionMapper {
    List<EvaluationMaterialDeletionTarget> listDeletionPreviewTargets(@Param("criteria") EvaluationMaterialDeletionSearchCriteria criteria);
    long countDeletionPreviewTargets(@Param("criteria") EvaluationMaterialDeletionSearchCriteria criteria);
    long countDeletableTargets(@Param("criteria") EvaluationMaterialDeletionSearchCriteria criteria);
    List<EvaluationMaterialDeletionTarget> listDeletionCandidates(@Param("request") EvaluationMaterialDeletionRequest request);
    void insertDeletionBatchJob(@Param("batchJobId") String batchJobId,
                                @Param("request") EvaluationMaterialDeletionRequest request,
                                @Param("totalCount") int totalCount,
                                @Param("successCount") int successCount,
                                @Param("failureCount") int failureCount,
                                @Param("excludedCount") int excludedCount,
                                @Param("requestedBy") Long requestedBy,
                                @Param("requestId") String requestId);
    void insertBatchJobItem(@Param("batchJobId") String batchJobId,
                            @Param("targetRef") String targetRef,
                            @Param("resultStatus") String resultStatus,
                            @Param("errorCode") String errorCode,
                            @Param("errorMessage") String errorMessage,
                            @Param("excludedReason") String excludedReason,
                            @Param("requestId") String requestId,
                            @Param("createdBy") Long createdBy);
    void updateBatchJobCounts(@Param("batchJobId") String batchJobId,
                              @Param("successCount") int successCount,
                              @Param("failureCount") int failureCount,
                              @Param("excludedCount") int excludedCount,
                              @Param("updatedBy") Long updatedBy);
    int markEvaluationMaterialDeleted(@Param("evaluationMaterialId") Long evaluationMaterialId,
                                      @Param("deleteReason") String deleteReason,
                                      @Param("requestId") String requestId,
                                      @Param("deletedBy") Long deletedBy);
}
