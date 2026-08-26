package kr.ac.knue.commonfoundation.batch;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchDefinitionMapper {
    List<BatchDefinitionRow> listBatchDefinitions(@Param("criteria") BatchDefinitionSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countBatchDefinitions(@Param("criteria") BatchDefinitionSearchCriteria criteria);
    BatchDefinitionRow findBatchDefinition(@Param("batchId") String batchId);
    int existsUser(@Param("userId") Long userId);
    int existsBatchDefinition(@Param("batchId") String batchId);
    void upsertBatchDefinition(@Param("request") BatchDefinitionRequest request, @Param("userId") Long userId,
            @Param("requestId") String requestId);
    void upsertBatchParameters(@Param("batchId") String batchId, @Param("parameterJson") String parameterJson,
            @Param("userId") Long userId, @Param("requestId") String requestId);
    void deleteDependenciesForBatch(@Param("batchId") String batchId);
    void insertDependency(@Param("predecessorBatchId") String predecessorBatchId,
            @Param("successorBatchId") String successorBatchId, @Param("userId") Long userId,
            @Param("requestId") String requestId);
    List<String> listPredecessorBatchIds(@Param("batchId") String batchId);
    List<String> listSuccessorBatchIds(@Param("batchId") String batchId);
}
