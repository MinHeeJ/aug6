package kr.ac.knue.commonfoundation.basic46;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScoreRecalculationMapper {
    List<ScoreRecalculationRow> listScoreRecalculations(@Param("criteria") ScoreRecalculationSearchCriteria criteria);
    long countScoreRecalculations(@Param("criteria") ScoreRecalculationSearchCriteria criteria);
    List<ScoreRecalculationCandidate> listRecalculationCandidates(@Param("request") ScoreRecalculationRequest request);
    ScoreFormulaSnapshot findFormulaSnapshot(@Param("formulaVersionId") Long formulaVersionId,
                                             @Param("evaluationYear") String evaluationYear);
    void insertRecalculationBatchJob(@Param("batchJobId") String batchJobId,
                                     @Param("request") ScoreRecalculationRequest request,
                                     @Param("parsedFormulaVersionId") Long parsedFormulaVersionId,
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
    int insertScoreGeneration(@Param("candidate") ScoreRecalculationCandidate candidate,
                              @Param("formulaVersionId") Long formulaVersionId,
                              @Param("previousScore") BigDecimal previousScore,
                              @Param("recalculatedScore") BigDecimal recalculatedScore,
                              @Param("selectionReason") String selectionReason,
                              @Param("batchJobId") String batchJobId,
                              @Param("requestId") String requestId,
                              @Param("createdBy") Long createdBy);
    int updateEvaluationMaterialScore(@Param("evaluationMaterialId") Long evaluationMaterialId,
                                      @Param("recalculatedScore") BigDecimal recalculatedScore,
                                      @Param("batchJobId") String batchJobId,
                                      @Param("generationNo") Integer generationNo,
                                      @Param("requestId") String requestId,
                                      @Param("updatedBy") Long updatedBy);
}
