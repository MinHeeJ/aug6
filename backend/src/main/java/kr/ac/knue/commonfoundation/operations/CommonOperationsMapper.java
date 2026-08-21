package kr.ac.knue.commonfoundation.operations;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonOperationsMapper {
    List<MenuExposureSetting> listMenuExposureSettings();
    int existsMenu(@Param("menuId") Long menuId);
    void updateMenuExposure(@Param("menuId") Long menuId, @Param("systemUseYn") String systemUseYn,
            @Param("exposureStartAt") java.time.LocalDateTime exposureStartAt,
            @Param("exposureEndAt") java.time.LocalDateTime exposureEndAt,
            @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    MenuExposureSetting findMenuExposure(@Param("menuId") Long menuId);

    List<DetailCodeUsageSetting> listDetailCodeUsageSettings(@Param("groupId") String groupId);
    int existsDetailCode(@Param("groupId") String groupId, @Param("codeValue") String codeValue);
    void updateDetailCodeUsage(@Param("groupId") String groupId, @Param("codeValue") String codeValue,
            @Param("systemUseYn") String systemUseYn, @Param("validStartDate") java.time.LocalDate validStartDate,
            @Param("validEndDate") java.time.LocalDate validEndDate, @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason);
    DetailCodeUsageSetting findDetailCodeUsage(@Param("groupId") String groupId, @Param("codeValue") String codeValue);
    List<DetailCodeUsageSetting> listActiveDetailCodeOptions(@Param("groupId") String groupId);

    List<CommonSettingRow> listCommonSettings();
    void upsertCommonSetting(@Param("settingKey") String settingKey, @Param("settingValue") String settingValue,
            @Param("settingUnit") String settingUnit, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);

    List<BaseYearSetting> listBaseYearSettings();
    void upsertBaseYearSetting(@Param("baseYear") Integer baseYear, @Param("currentEvaluationYear") Integer currentEvaluationYear,
            @Param("defaultSearchYear") Integer defaultSearchYear, @Param("copyRequestedYn") String copyRequestedYn,
            @Param("initializeRequestedYn") String initializeRequestedYn, @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason);
    BaseYearSetting findBaseYearSetting(@Param("baseYear") Integer baseYear);
    void insertPreparationHistory(@Param("baseYear") Integer baseYear, @Param("copyRequestedYn") String copyRequestedYn,
            @Param("initializeRequestedYn") String initializeRequestedYn, @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);
    StandardPreparationHistory latestPreparationHistory(@Param("baseYear") Integer baseYear);
    Integer findDefaultSearchYear();
}
