package kr.ac.knue.commonfoundation.basic45;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationBatchResultMapper {
    List<EvaluationBatchResultRow> listResults(@Param("criteria") EvaluationBatchResultSearchCriteria criteria);
    long countResults(@Param("criteria") EvaluationBatchResultSearchCriteria criteria);
    List<EvaluationBatchResultErrorRow> listErrors(@Param("batchId") String batchId,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);
    long countErrors(@Param("batchId") String batchId);
}
