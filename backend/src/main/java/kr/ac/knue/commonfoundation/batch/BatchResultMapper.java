package kr.ac.knue.commonfoundation.batch;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchResultMapper {
    List<BatchResultRow> listBatchResults(@Param("criteria") BatchResultSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countBatchResults(@Param("criteria") BatchResultSearchCriteria criteria);
    BatchResultLogResponse findBatchResultLog(@Param("executionId") String executionId);
}
