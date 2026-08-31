package kr.ac.knue.commonfoundation.basic33;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationManagementItemMapper {
    List<EvaluationManagementItemRow> listEvaluationManagementItems(@Param("criteria") EvaluationManagementItemSearchCriteria criteria);

    long countEvaluationManagementItems(@Param("criteria") EvaluationManagementItemSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    Long findElementId(@Param("ruleVersionId") Long ruleVersionId,
                       @Param("areaCode") String areaCode,
                       @Param("itemCode") String itemCode,
                       @Param("evaluationYear") String evaluationYear,
                       @Param("elementCode") String elementCode);

    EvaluationManagementItemRow findByKey(@Param("elementId") Long elementId,
                                          @Param("managementItemCode") String managementItemCode);

    void upsertEvaluationManagementItem(@Param("request") SaveEvaluationManagementItemRequest request,
                                        @Param("elementId") Long elementId,
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
