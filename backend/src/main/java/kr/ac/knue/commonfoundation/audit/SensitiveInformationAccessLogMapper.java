package kr.ac.knue.commonfoundation.audit;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SensitiveInformationAccessLogMapper {
    List<SensitiveInformationAccessLogRow> listSensitiveInformationAccessLogs(
            @Param("criteria") SensitiveInformationAccessLogSearchCriteria criteria,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long countSensitiveInformationAccessLogs(@Param("criteria") SensitiveInformationAccessLogSearchCriteria criteria);
}
