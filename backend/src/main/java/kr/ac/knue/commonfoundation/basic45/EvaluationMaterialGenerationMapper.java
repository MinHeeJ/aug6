package kr.ac.knue.commonfoundation.basic45;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationMaterialGenerationMapper {
    List<EvaluationMaterialGenerationTarget> listGenerationTargets(@Param("criteria") EvaluationMaterialGenerationSearchCriteria criteria);
    long countGenerationTargets(@Param("criteria") EvaluationMaterialGenerationSearchCriteria criteria);
    long nextGenerationSequence();
    int insertBatchRequest(@Param("batchId") String batchId,
                           @Param("jobType") String jobType,
                           @Param("targetConditionJson") String targetConditionJson,
                           @Param("requestUserId") Long requestUserId,
                           @Param("requestId") String requestId);
    int insertEvaluationMaterial(@Param("target") EvaluationMaterialGenerationTarget target,
                                 @Param("batchId") String batchId,
                                 @Param("requestId") String requestId,
                                 @Param("requestUserId") Long requestUserId);
    int insertBatchResult(@Param("batchId") String batchId,
                          @Param("jobType") String jobType,
                          @Param("totalCount") int totalCount,
                          @Param("successCount") int successCount,
                          @Param("failureCount") int failureCount,
                          @Param("excludedCount") int excludedCount,
                          @Param("requestId") String requestId);
}
