package kr.ac.knue.commonfoundation.basic33;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AreaElementSystemMapper {
    List<AreaElementSystemRow> listAreaElementSystems(@Param("criteria") AreaElementSystemSearchCriteria criteria);

    long countAreaElementSystems(@Param("criteria") AreaElementSystemSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    AreaElementSystemTargetIds findAreaItemElementIds(@Param("ruleVersionId") Long ruleVersionId,
                                                      @Param("areaCode") String areaCode,
                                                      @Param("itemCode") String itemCode,
                                                      @Param("evaluationYear") String evaluationYear,
                                                      @Param("elementCode") String elementCode);

    AreaElementSystemRow findByKey(@Param("areaId") Long areaId,
                                   @Param("itemId") Long itemId,
                                   @Param("elementId") Long elementId,
                                   @Param("targetScope") String targetScope);

    void upsertAreaElementSystem(@Param("request") SaveAreaElementSystemRequest request,
                                 @Param("areaId") Long areaId,
                                 @Param("itemId") Long itemId,
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
