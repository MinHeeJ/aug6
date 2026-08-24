package kr.ac.knue.commonfoundation.permissionops;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PermissionChangeHistoryMapper {
    List<PermissionChangeHistoryRow> listPermissionChangeHistory(@Param("criteria") PermissionChangeHistorySearchCriteria criteria);

    long countPermissionChangeHistory(@Param("criteria") PermissionChangeHistorySearchCriteria criteria);

    void insertPermissionChangeHistory(@Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("reason") String reason);
}
