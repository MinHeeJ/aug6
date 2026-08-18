package kr.ac.knue.commonfoundation.menus;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuStructureMapper {
    List<MenuTreeRow> findMenusForTree(@Param("filter") String filter);

    int existsMenu(@Param("menuId") Long menuId);

    int isDescendant(@Param("ancestorMenuId") Long ancestorMenuId, @Param("candidateMenuId") Long candidateMenuId);

    int countSiblingsUnderParent(@Param("parentMenuId") Long parentMenuId, @Param("menuIds") List<Long> menuIds);

    void updateParent(@Param("menuId") Long menuId,
                      @Param("parentMenuId") Long parentMenuId,
                      @Param("updatedBy") Long updatedBy,
                      @Param("changeReason") String changeReason);

    void updateDisplayOrder(@Param("menuId") Long menuId,
                            @Param("parentMenuId") Long parentMenuId,
                            @Param("displayOrder") int displayOrder,
                            @Param("updatedBy") Long updatedBy,
                            @Param("changeReason") String changeReason);

    MenuTreeRow findMenu(@Param("menuId") Long menuId);

    List<MenuTreeRow> findMenusByParent(@Param("parentMenuId") Long parentMenuId);
}
