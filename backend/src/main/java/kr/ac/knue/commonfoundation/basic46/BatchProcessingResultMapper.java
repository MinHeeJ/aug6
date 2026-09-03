package kr.ac.knue.commonfoundation.basic46;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchProcessingResultMapper {
    List<BatchProcessingResultRow> listBatchProcessingResults(
            @Param("criteria") BatchProcessingResultSearchCriteria criteria);

    long countBatchProcessingResults(@Param("criteria") BatchProcessingResultSearchCriteria criteria);

    List<BatchProcessingResultErrorRow> listBatchProcessingResultErrors(@Param("batchId") String batchId);
}
