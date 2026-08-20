package kr.ac.knue.commonfoundation.settings;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonSystemSettingsMapper {
    List<CommonSystemSettingRow> listCommonSystemSettings(@Param("keys") List<String> keys);

    void upsertCommonSystemSetting(@Param("settingKey") String settingKey,
                                   @Param("settingValue") String settingValue,
                                   @Param("unit") String unit,
                                   @Param("updatedBy") Long updatedBy,
                                   @Param("changeReason") String changeReason);

    default void insertUserSpecificSetting(Object first, Object second, Object third) {
        throw new UnsupportedOperationException("common_system_settings는 사용자별 환경값을 생성하지 않습니다.");
    }

    default void insertBusinessSpecificSetting(Object first, Object second, Object third) {
        throw new UnsupportedOperationException("common_system_settings는 업무별 환경값을 생성하지 않습니다.");
    }
}
