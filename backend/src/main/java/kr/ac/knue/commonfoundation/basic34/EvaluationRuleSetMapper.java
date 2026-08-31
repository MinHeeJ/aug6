package kr.ac.knue.commonfoundation.basic34;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationRuleSetMapper {
    List<EvaluationRuleSetRow> listEvaluationRuleSets(@Param("criteria") EvaluationRuleSetSearchCriteria criteria);
    long countEvaluationRuleSets(@Param("criteria") EvaluationRuleSetSearchCriteria criteria);
    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);
    EvaluationRuleSetRow findByKey(@Param("request") SaveEvaluationRuleSetRequest request);
    void upsertEvaluationRuleSet(@Param("request") SaveEvaluationRuleSetRequest request, @Param("updatedBy") Long updatedBy);
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
