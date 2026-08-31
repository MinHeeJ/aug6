package kr.ac.knue.commonfoundation.basic32;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RejectionReasonMapper {
    List<RejectionReasonRow> listReasons(@Param("criteria") RejectionReasonSearchCriteria criteria);

    long countReasons(@Param("criteria") RejectionReasonSearchCriteria criteria);

    RejectionReasonRow findByKey(@Param("businessType") String businessType,
                                 @Param("reasonCode") String reasonCode);

    void upsertRejectionReason(@Param("businessType") String businessType,
                               @Param("reasonCode") String reasonCode,
                               @Param("standardMessage") String standardMessage,
                               @Param("additionalOpinionAllowedYn") String additionalOpinionAllowedYn,
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
