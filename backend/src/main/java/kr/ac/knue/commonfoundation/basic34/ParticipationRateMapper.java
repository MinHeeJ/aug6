package kr.ac.knue.commonfoundation.basic34;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipationRateMapper {
    List<ParticipationRateRow> listParticipationRates(@Param("criteria") ParticipationRateSearchCriteria criteria);
    long countParticipationRates(@Param("criteria") ParticipationRateSearchCriteria criteria);
    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);
    Boolean managementItemBelongsToRuleVersion(@Param("ruleVersionId") Long ruleVersionId, @Param("managementItemId") Long managementItemId);
    ParticipationRateRow findByKey(@Param("request") SaveParticipationRateRequest request);
    void upsertParticipationRate(@Param("request") SaveParticipationRateRequest request, @Param("updatedBy") Long updatedBy);
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
