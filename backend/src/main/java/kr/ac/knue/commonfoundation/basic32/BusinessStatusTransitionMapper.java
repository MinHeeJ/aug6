package kr.ac.knue.commonfoundation.basic32;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessStatusTransitionMapper {
    List<BusinessStatusTransitionRow> listTransitions(@Param("criteria") BusinessStatusTransitionSearchCriteria criteria);

    long countTransitions(@Param("criteria") BusinessStatusTransitionSearchCriteria criteria);

    BusinessStatusTransitionRow findByKey(@Param("businessType") String businessType,
                                          @Param("definitionVersion") String definitionVersion,
                                          @Param("fromStatusCode") String fromStatusCode,
                                          @Param("toStatusCode") String toStatusCode,
                                          @Param("executorRoleCode") String executorRoleCode);

    int statusCodeExists(@Param("businessType") String businessType,
                         @Param("definitionVersion") String definitionVersion,
                         @Param("statusCode") String statusCode);

    int roleExists(@Param("roleCode") String roleCode);

    void upsertDraftTransition(@Param("definitionVersion") String definitionVersion,
                               @Param("businessType") String businessType,
                               @Param("fromStatusCode") String fromStatusCode,
                               @Param("toStatusCode") String toStatusCode,
                               @Param("executorRoleCode") String executorRoleCode,
                               @Param("opinionRequiredYn") String opinionRequiredYn,
                               @Param("attachmentRequiredYn") String attachmentRequiredYn,
                               @Param("cancellableYn") String cancellableYn,
                               @Param("changeReason") String changeReason,
                               @Param("updatedBy") Long updatedBy);

    void insertChangeHistory(@Param("targetBusiness") String targetBusiness,
                             @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType,
                             @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue,
                             @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy,
                             @Param("changeReason") String changeReason);
}
