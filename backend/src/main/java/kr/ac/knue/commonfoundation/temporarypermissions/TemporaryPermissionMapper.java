package kr.ac.knue.commonfoundation.temporarypermissions;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TemporaryPermissionMapper {
    List<TemporaryPermissionRow> listTemporaryPermissions(@Param("criteria") TemporaryPermissionSearchCriteria criteria);

    long countTemporaryPermissions(@Param("criteria") TemporaryPermissionSearchCriteria criteria);

    int existsUser(@Param("userId") Long userId);

    TemporaryPermissionRow findById(@Param("temporaryPermissionId") Long temporaryPermissionId);

    void insertTemporaryPermission(@Param("userId") Long userId,
                                   @Param("workDataRef") String workDataRef,
                                   @Param("functionType") String functionType,
                                   @Param("validStartAt") java.time.LocalDateTime validStartAt,
                                   @Param("validEndAt") java.time.LocalDateTime validEndAt,
                                   @Param("changeReason") String changeReason,
                                   @Param("createdBy") Long createdBy);

    Long lastInsertedId();

    void expireElapsedTemporaryPermissions();

    int countActiveTemporaryPermission(@Param("userId") Long userId,
                                       @Param("workDataRef") String workDataRef,
                                       @Param("functionType") String functionType);
}
