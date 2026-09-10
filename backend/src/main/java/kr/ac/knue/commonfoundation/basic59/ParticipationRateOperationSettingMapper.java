package kr.ac.knue.commonfoundation.basic59;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipationRateOperationSettingMapper {
    List<ParticipationRateOperationSettingRow> listParticipationRateOperationSettings(@Param("criteria") ParticipationRateOperationSettingSearchCriteria criteria);

    long countParticipationRateOperationSettings(@Param("criteria") ParticipationRateOperationSettingSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    String findRuleVersionEvaluationYear(@Param("ruleVersionId") Long ruleVersionId);

    ParticipationRateOperationSettingRow findByBusinessKey(@Param("ruleVersionId") Long ruleVersionId,
                                                           @Param("evaluationYear") String evaluationYear,
                                                           @Param("achievementAreaCode") String achievementAreaCode,
                                                           @Param("achievementCategoryCode") String achievementCategoryCode,
                                                           @Param("managementItemCode") String managementItemCode,
                                                           @Param("researcherCountBand") String researcherCountBand,
                                                           @Param("participationTypeCode") String participationTypeCode);

    ParticipationRateOperationSettingRow upsertParticipationRateOperationSetting(@Param("request") SaveParticipationRateOperationSettingRequest request,
                                                                                 @Param("rate") SaveParticipationRateOperationSettingRequest.Rate rate,
                                                                                 @Param("updatedBy") Long updatedBy,
                                                                                 @Param("requestId") String requestId);

    void insertChangeHistory(@Param("targetBusiness") String targetBusiness,
                             @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType,
                             @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue,
                             @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy,
                             @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);
}
