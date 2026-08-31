package kr.ac.knue.commonfoundation.audit;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PermissionChangeLogMapper {
    List<PermissionChangeLogRow> listPermissionChangeLogs(
            @Param("criteria") PermissionChangeLogSearchCriteria criteria,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long countPermissionChangeLogs(@Param("criteria") PermissionChangeLogSearchCriteria criteria);
}
