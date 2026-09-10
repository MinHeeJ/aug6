package kr.ac.knue.commonfoundation.basic59;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationElementManagementItemMapper {
    List<EvaluationElementManagementItemRow> listEvaluationElementManagementItems(@Param("criteria") EvaluationElementManagementItemSearchCriteria criteria);

    long countEvaluationElementManagementItems(@Param("criteria") EvaluationElementManagementItemSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    EvaluationElementManagementItemRow findByBusinessKey(@Param("ruleVersionId") Long ruleVersionId,
                                                         @Param("evaluationYear") String evaluationYear,
                                                         @Param("areaCode") String areaCode,
                                                         @Param("elementCode") String elementCode,
                                                         @Param("managementItemCode") String managementItemCode);

    EvaluationElementManagementItemRow upsertEvaluationElementManagementItem(@Param("request") SaveEvaluationElementManagementItemRequest request,
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
