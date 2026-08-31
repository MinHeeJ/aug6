package kr.ac.knue.commonfoundation.basic34;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalculationFormulaMapper {
    List<CalculationFormulaRow> listCalculationFormulas(@Param("criteria") CalculationFormulaSearchCriteria criteria);
    long countCalculationFormulas(@Param("criteria") CalculationFormulaSearchCriteria criteria);
    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);
    CalculationFormulaRow findByKey(@Param("request") SaveCalculationFormulaRequest request);
    void upsertCalculationFormula(@Param("request") SaveCalculationFormulaRequest request, @Param("updatedBy") Long updatedBy);
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
