package kr.ac.knue.commonfoundation.businessperiod;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessPeriodMapper {
    List<BusinessPeriodSettingRow> listEvaluationDates(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    long countEvaluationDates(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    BusinessPeriodSettingRow findEvaluationDateById(@Param("settingId") Long settingId);
    BusinessPeriodSettingRow insertEvaluationDate(@Param("request") SaveEvaluationDateRequest request, @Param("userId") Long userId);
    BusinessPeriodSettingRow updateEvaluationDate(@Param("request") SaveEvaluationDateRequest request, @Param("userId") Long userId);
    int countOverlappingEvaluationDates(@Param("settingId") Long settingId, @Param("evaluationYear") String evaluationYear,
                                        @Param("areaCode") String areaCode, @Param("organizationCode") String organizationCode,
                                        @Param("userTypeCode") String userTypeCode, @Param("startAt") LocalDateTime startAt,
                                        @Param("endAt") LocalDateTime endAt);
    int existsAuthorizedEvaluationOrganization(@Param("userId") Long userId, @Param("organizationCode") String organizationCode);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType, @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);

    List<BusinessPeriodSettingRow> listInputPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    long countInputPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    BusinessPeriodSettingRow findInputPeriodById(@Param("settingId") Long settingId);
    BusinessPeriodSettingRow insertInputPeriod(@Param("request") SaveInputPeriodRequest request, @Param("userId") Long userId);
    BusinessPeriodSettingRow updateInputPeriod(@Param("request") SaveInputPeriodRequest request, @Param("userId") Long userId);
    int countOverlappingInputPeriods(@Param("settingId") Long settingId, @Param("evaluationYear") String evaluationYear,
                                     @Param("areaCode") String areaCode, @Param("organizationCode") String organizationCode,
                                     @Param("userTypeCode") String userTypeCode, @Param("startAt") LocalDateTime startAt,
                                     @Param("endAt") LocalDateTime endAt);

    List<BusinessPeriodSettingRow> listModificationPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    long countModificationPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    BusinessPeriodSettingRow findModificationPeriodById(@Param("settingId") Long settingId);
    BusinessPeriodSettingRow insertModificationPeriod(@Param("request") SaveModificationPeriodRequest request,
                                                      @Param("userId") Long userId);
    BusinessPeriodSettingRow updateModificationPeriod(@Param("request") SaveModificationPeriodRequest request,
                                                      @Param("userId") Long userId);
    int countOverlappingModificationPeriods(@Param("settingId") Long settingId, @Param("evaluationYear") String evaluationYear,
                                            @Param("areaCode") String areaCode, @Param("organizationCode") String organizationCode,
                                            @Param("userTypeCode") String userTypeCode, @Param("startAt") LocalDateTime startAt,
                                            @Param("endAt") LocalDateTime endAt);

    List<BusinessPeriodSettingRow> listDepartmentChairConfirmPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    long countDepartmentChairConfirmPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    BusinessPeriodSettingRow findDepartmentChairConfirmPeriodById(@Param("settingId") Long settingId);
    BusinessPeriodSettingRow insertDepartmentChairConfirmPeriod(@Param("request") SaveDepartmentChairConfirmPeriodRequest request,
                                                               @Param("userId") Long userId);
    BusinessPeriodSettingRow updateDepartmentChairConfirmPeriod(@Param("request") SaveDepartmentChairConfirmPeriodRequest request,
                                                               @Param("userId") Long userId);
    int countOverlappingDepartmentChairConfirmPeriods(@Param("settingId") Long settingId, @Param("evaluationYear") String evaluationYear,
                                                      @Param("areaCode") String areaCode, @Param("organizationCode") String organizationCode,
                                                      @Param("userTypeCode") String userTypeCode, @Param("startAt") LocalDateTime startAt,
                                                      @Param("endAt") LocalDateTime endAt);

    List<BusinessPeriodSettingRow> listBusinessPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    long countBusinessPeriods(@Param("criteria") BusinessPeriodSearchCriteria criteria);
    BusinessPeriodSettingRow findBusinessPeriodById(@Param("settingId") Long settingId);
    BusinessPeriodSettingRow insertBusinessPeriod(@Param("request") SaveBusinessPeriodRequest request, @Param("userId") Long userId);
    BusinessPeriodSettingRow updateBusinessPeriod(@Param("request") SaveBusinessPeriodRequest request, @Param("userId") Long userId);
    int countOverlappingBusinessPeriods(@Param("settingId") Long settingId, @Param("evaluationYear") String evaluationYear,
                                        @Param("areaCode") String areaCode, @Param("organizationCode") String organizationCode,
                                        @Param("userTypeCode") String userTypeCode, @Param("startAt") LocalDateTime startAt,
                                        @Param("endAt") LocalDateTime endAt);
}
