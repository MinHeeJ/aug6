package kr.ac.knue.commonfoundation.basic33;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationAreaMapper {
    List<EvaluationAreaRow> listEvaluationAreas(@Param("criteria") EvaluationAreaSearchCriteria criteria);

    long countEvaluationAreas(@Param("criteria") EvaluationAreaSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    EvaluationAreaRow findByKey(@Param("ruleVersionId") Long ruleVersionId,
                                @Param("areaCode") String areaCode);

    void upsertEvaluationArea(@Param("request") SaveEvaluationAreaRequest request,
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
