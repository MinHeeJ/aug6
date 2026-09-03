package kr.ac.knue.commonfoundation.basic45;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationMaterialDeletionMapper {
    List<EvaluationMaterialDeletionTarget> listDeletionTargets(@Param("criteria") EvaluationMaterialDeletionSearchCriteria criteria);
    long countDeletionTargets(@Param("criteria") EvaluationMaterialDeletionSearchCriteria criteria);
    long nextDeletionSequence();
    int insertBatchRequest(@Param("batchId") String batchId,
                           @Param("jobType") String jobType,
                           @Param("targetConditionJson") String targetConditionJson,
                           @Param("requestUserId") Long requestUserId,
                           @Param("requestId") String requestId);
    int logicalDeleteEvaluationMaterials(@Param("criteria") EvaluationMaterialDeletionSearchCriteria criteria,
                                         @Param("deleteReason") String deleteReason,
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
