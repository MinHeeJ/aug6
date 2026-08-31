package kr.ac.knue.commonfoundation.basic33;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationElementMapper {
    List<EvaluationElementRow> listEvaluationElements(@Param("criteria") EvaluationElementSearchCriteria criteria);

    long countEvaluationElements(@Param("criteria") EvaluationElementSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    Long findItemId(@Param("ruleVersionId") Long ruleVersionId,
                    @Param("areaCode") String areaCode,
                    @Param("itemCode") String itemCode);

    EvaluationElementRow findByKey(@Param("itemId") Long itemId,
                                   @Param("evaluationYear") String evaluationYear,
                                   @Param("elementCode") String elementCode);

    void upsertEvaluationElement(@Param("request") SaveEvaluationElementRequest request,
                                 @Param("itemId") Long itemId,
                                 @Param("updatedBy") Long updatedBy);

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
