package kr.ac.knue.commonfoundation.basic46;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationMaterialGenerationMapper {
    List<EvaluationMaterialGenerationTarget> listGenerationTargets(@Param("criteria") EvaluationMaterialGenerationSearchCriteria criteria);
    long countGenerationTargets(@Param("criteria") EvaluationMaterialGenerationSearchCriteria criteria);
    List<EvaluationMaterialGenerationTarget> listSourceCandidates(@Param("request") EvaluationMaterialGenerationRequest request);
    int existingMaterialCount(@Param("sourceAchievementId") Long sourceAchievementId,
                              @Param("evaluationYear") String evaluationYear,
                              @Param("areaCode") String areaCode,
                              @Param("targetUserId") Long targetUserId);
    void insertBatchJob(@Param("batchJobId") String batchJobId,
                        @Param("request") EvaluationMaterialGenerationRequest request,
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
    int insertEvaluationMaterial(@Param("batchJobId") String batchJobId,
                                 @Param("candidate") EvaluationMaterialGenerationTarget candidate,
                                 @Param("requestId") String requestId,
                                 @Param("createdBy") Long createdBy);
}
