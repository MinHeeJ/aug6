package kr.ac.knue.commonfoundation.basic32;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessStatusCodeMapper {
    List<BusinessStatusCodeRow> listStatusCodes(@Param("criteria") BusinessStatusCodeSearchCriteria criteria);

    long countStatusCodes(@Param("criteria") BusinessStatusCodeSearchCriteria criteria);

    BusinessStatusCodeRow findByKey(@Param("businessType") String businessType,
                                    @Param("definitionVersion") String definitionVersion,
                                    @Param("statusCode") String statusCode);

    int confirmedCodeExists(@Param("businessType") String businessType,
                            @Param("statusCode") String statusCode);

    void upsertDraftStatusCode(@Param("definitionVersion") String definitionVersion,
                               @Param("businessType") String businessType,
                               @Param("statusCode") String statusCode,
                               @Param("displayName") String displayName,
                               @Param("systemUseYn") String systemUseYn,
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
