package kr.ac.knue.commonfoundation.basic32;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationOrganizationMappingMapper {
    List<EvaluationOrganizationMappingRow> listMappings(@Param("criteria") EvaluationOrganizationMappingSearchCriteria criteria);

    long countMappings(@Param("criteria") EvaluationOrganizationMappingSearchCriteria criteria);

    int existsUser(@Param("userId") Long userId);

    int existsOrganization(@Param("organizationCode") String organizationCode);

    EvaluationOrganizationMappingRow findByKey(@Param("userId") Long userId,
                                                @Param("organizationCode") String organizationCode,
                                                @Param("businessType") String businessType);

    void upsertMapping(@Param("userId") Long userId,
                       @Param("organizationCode") String organizationCode,
                       @Param("businessType") String businessType,
                       @Param("dataScope") String dataScope,
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
