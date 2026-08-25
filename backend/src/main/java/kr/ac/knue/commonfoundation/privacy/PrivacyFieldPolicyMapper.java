package kr.ac.knue.commonfoundation.privacy;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrivacyFieldPolicyMapper {
    List<PrivacyFieldPolicyRow> listPrivacyFieldPolicies(@Param("criteria") PrivacyFieldPolicySearchCriteria criteria);

    long countPrivacyFieldPolicies(@Param("criteria") PrivacyFieldPolicySearchCriteria criteria);

    PrivacyFieldPolicyRow findByFieldKey(@Param("fieldKey") String fieldKey);

    void upsertPrivacyFieldPolicy(@Param("fieldKey") String fieldKey,
                                  @Param("privacyGrade") String privacyGrade,
                                  @Param("encryptionRequiredYn") String encryptionRequiredYn,
                                  @Param("maskingRule") String maskingRule,
                                  @Param("logExclusionYn") String logExclusionYn,
                                  @Param("changeReason") String changeReason,
                                  @Param("updatedBy") Long updatedBy);
}
