package kr.ac.knue.commonfoundation.permissions;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuPermissionMapper {
    List<MenuPermissionRow> listMenuPermissions(@Param("criteria") MenuPermissionSearchCriteria criteria);

    int countMenuPermissions(@Param("criteria") MenuPermissionSearchCriteria criteria);

    int existsMenu(@Param("menuId") Long menuId);

    int existsTarget(@Param("targetType") String targetType, @Param("targetId") String targetId);

    void upsertPermission(@Param("targetType") String targetType,
                          @Param("targetId") String targetId,
                          @Param("menuId") Long menuId,
                          @Param("accessAllowed") String accessAllowed,
                          @Param("updatedBy") Long updatedBy,
                          @Param("changeReason") String changeReason);

    MenuPermissionRow findPermission(@Param("targetType") String targetType,
                                     @Param("targetId") String targetId,
                                     @Param("menuId") Long menuId);
}
