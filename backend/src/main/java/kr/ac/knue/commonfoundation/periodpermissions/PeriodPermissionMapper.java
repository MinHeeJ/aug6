package kr.ac.knue.commonfoundation.periodpermissions;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PeriodPermissionMapper {
    List<PeriodPermissionRow> listPeriodPermissions(@Param("criteria") PeriodPermissionSearchCriteria criteria);

    long countPeriodPermissions(@Param("criteria") PeriodPermissionSearchCriteria criteria);

    int existsFunctionPermission(@Param("functionPermissionId") Long functionPermissionId);

    PeriodPermissionRow findByKey(@Param("businessPeriodId") String businessPeriodId,
                                  @Param("functionPermissionId") Long functionPermissionId);

    void upsertPeriodPermission(@Param("businessPeriodId") String businessPeriodId,
                                @Param("functionPermissionId") Long functionPermissionId,
                                @Param("effectiveStartAt") LocalDateTime effectiveStartAt,
                                @Param("effectiveEndAt") LocalDateTime effectiveEndAt,
                                @Param("periodState") String periodState,
                                @Param("updatedBy") Long updatedBy,
                                @Param("changeReason") String changeReason);
}
