package kr.ac.knue.commonfoundation.menus;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuUsageMapper {
    List<MenuUsageRow> listMenuUsageSettings(@Param("criteria") MenuUsageSearchCriteria criteria);

    int countMenuUsageSettings(@Param("criteria") MenuUsageSearchCriteria criteria);

    int existsMenu(@Param("menuId") Long menuId);

    void upsertMenuUsageSetting(@Param("menuId") Long menuId,
                                @Param("systemUseYn") String systemUseYn,
                                @Param("exposureStartAt") LocalDateTime exposureStartAt,
                                @Param("exposureEndAt") LocalDateTime exposureEndAt,
                                @Param("updatedBy") Long updatedBy,
                                @Param("changeReason") String changeReason);

    MenuUsageRow findMenuUsageSetting(@Param("menuId") Long menuId);

    default void updateMenuStructure(Object first, Object second, Object third, Object fourth) {
        throw new UnsupportedOperationException("menu_usage_settings 저장은 menus 구조를 변경하지 않습니다.");
    }

    default void updateMenuExecution(Object first, Object second, Object third, Object fourth) {
        throw new UnsupportedOperationException("menu_usage_settings 저장은 menu_execution_info를 변경하지 않습니다.");
    }

    default void updateMenuPermission(Object first, Object second, Object third, Object fourth) {
        throw new UnsupportedOperationException("menu_usage_settings 저장은 menu_permissions를 변경하지 않습니다.");
    }
}
