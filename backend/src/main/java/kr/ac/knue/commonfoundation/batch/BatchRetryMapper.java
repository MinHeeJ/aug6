package kr.ac.knue.commonfoundation.batch;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchRetryMapper {
    List<BatchRetryTargetRow> listBatchRetryTargets(@Param("criteria") BatchRetryTargetSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countBatchRetryTargets(@Param("criteria") BatchRetryTargetSearchCriteria criteria);
    int countFailedRetryTarget(@Param("originalExecutionId") String originalExecutionId,
            @Param("failedItemKey") String failedItemKey);
    String findOriginalBatchId(@Param("originalExecutionId") String originalExecutionId);
    void insertRetryExecution(@Param("retryExecutionId") String retryExecutionId,
            @Param("batchId") String batchId,
            @Param("processType") String processType,
            @Param("reason") String reason,
            @Param("operatorUserId") Long operatorUserId,
            @Param("originalExecutionId") String originalExecutionId,
            @Param("requestId") String requestId);
    void insertRetryResult(@Param("retryExecutionId") String retryExecutionId,
            @Param("originalExecutionId") String originalExecutionId,
            @Param("failedItemKey") String failedItemKey,
            @Param("retryReason") String retryReason,
            @Param("requestId") String requestId);
    BatchRetryResultRow findRetryResult(@Param("retryExecutionId") String retryExecutionId);

    default void updateOriginalExecutionResult(String originalExecutionId, Object ignored) {
        throw new UnsupportedOperationException("원실행 결과는 재처리에서 수정하지 않습니다.");
    }
}
