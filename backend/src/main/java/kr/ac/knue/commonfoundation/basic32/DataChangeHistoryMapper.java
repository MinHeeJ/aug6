package kr.ac.knue.commonfoundation.basic32;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataChangeHistoryMapper {
    List<DataChangeHistoryRow> listHistories(@Param("criteria") DataChangeHistorySearchCriteria criteria);

    long countHistories(@Param("criteria") DataChangeHistorySearchCriteria criteria);
}
