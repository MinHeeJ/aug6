package kr.ac.knue.commonfoundation.audit;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessProcessLogMapper {
    List<BusinessProcessLogRow> listBusinessProcessLogs(@Param("criteria") BusinessProcessLogSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countBusinessProcessLogs(@Param("criteria") BusinessProcessLogSearchCriteria criteria);
}
