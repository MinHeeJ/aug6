package kr.ac.knue.commonfoundation.functionpermissions;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FunctionPermissionMapper {
    List<FunctionPermissionRow> listFunctionPermissions(@Param("criteria") FunctionPermissionSearchCriteria criteria);

    long countFunctionPermissions(@Param("criteria") FunctionPermissionSearchCriteria criteria);

    int existsScreen(@Param("screenId") String screenId);

    int existsRole(@Param("roleCode") String roleCode);

    FunctionPermissionRow findByKey(@Param("screenId") String screenId,
                                    @Param("roleCode") String roleCode,
                                    @Param("functionType") String functionType);

    void upsertFunctionPermission(@Param("screenId") String screenId,
                                  @Param("roleCode") String roleCode,
                                  @Param("functionType") String functionType,
                                  @Param("permissionAllowed") String permissionAllowed,
                                  @Param("updatedBy") Long updatedBy,
                                  @Param("changeReason") String changeReason);
}
