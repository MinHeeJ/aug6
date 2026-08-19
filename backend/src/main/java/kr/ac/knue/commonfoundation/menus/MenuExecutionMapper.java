package kr.ac.knue.commonfoundation.menus;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuExecutionMapper {
    MenuExecutionRow findExecution(@Param("menuId") Long menuId);

    int existsActiveMenu(@Param("menuId") Long menuId);

    int countActiveScreenIdExceptMenu(@Param("screenId") String screenId, @Param("menuId") Long menuId);

    void updateMenuExecutionFields(@Param("menuId") Long menuId,
                                   @Param("menuName") String menuName,
                                   @Param("screenId") String screenId,
                                   @Param("url") String url,
                                   @Param("icon") String icon,
                                   @Param("businessCategory") String businessCategory,
                                   @Param("description") String description,
                                   @Param("updatedBy") Long updatedBy,
                                   @Param("changeReason") String changeReason);

    void upsertMenuExecutionInfo(@Param("menuId") Long menuId,
                                 @Param("screenId") String screenId,
                                 @Param("url") String url,
                                 @Param("icon") String icon,
                                 @Param("businessCategory") String businessCategory,
                                 @Param("description") String description,
                                 @Param("updatedBy") Long updatedBy);
}
