package kr.ac.knue.commonfoundation.batch;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchExecutionMapper {
    List<BatchExecutionRow> listBatchExecutions(@Param("criteria") BatchExecutionSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countBatchExecutions(@Param("criteria") BatchExecutionSearchCriteria criteria);
    BatchExecutionRow findBatchExecution(@Param("executionId") String executionId);
    int existsBatchDefinition(@Param("batchId") String batchId);
    void insertBatchExecution(@Param("row") BatchExecutionRow row);
    void stopBatchExecution(@Param("executionId") String executionId, @Param("reason") String reason,
            @Param("operatorUserId") Long operatorUserId, @Param("requestId") String requestId);
}
