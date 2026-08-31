package kr.ac.knue.commonfoundation.basic33;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationItemMapper {
    List<EvaluationItemRow> listEvaluationItems(@Param("criteria") EvaluationItemSearchCriteria criteria);

    long countEvaluationItems(@Param("criteria") EvaluationItemSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    Long findAreaId(@Param("ruleVersionId") Long ruleVersionId,
                    @Param("areaCode") String areaCode);

    int existsParentItem(@Param("areaId") Long areaId,
                         @Param("parentItemCode") String parentItemCode);

    EvaluationItemRow findByKey(@Param("areaId") Long areaId,
                                @Param("itemCode") String itemCode);

    void upsertEvaluationItem(@Param("request") SaveEvaluationItemRequest request,
                              @Param("areaId") Long areaId,
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
