package kr.ac.knue.commonfoundation.basic59;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ManagementItemEvaluationScoreMapper {
    List<ManagementItemEvaluationScoreRow> listManagementItemEvaluationScores(@Param("criteria") ManagementItemEvaluationScoreSearchCriteria criteria);

    long countManagementItemEvaluationScores(@Param("criteria") ManagementItemEvaluationScoreSearchCriteria criteria);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);

    String findRuleVersionEvaluationYear(@Param("ruleVersionId") Long ruleVersionId);

    boolean managementItemMatchesCategory(@Param("ruleVersionId") Long ruleVersionId,
                                          @Param("achievementAreaCode") String achievementAreaCode,
                                          @Param("achievementCategoryCode") String achievementCategoryCode,
                                          @Param("managementItemCode") String managementItemCode);

    ManagementItemEvaluationScoreRow findByBusinessKey(@Param("ruleVersionId") Long ruleVersionId,
                                                       @Param("evaluationYear") String evaluationYear,
                                                       @Param("achievementAreaCode") String achievementAreaCode,
                                                       @Param("managementItemCode") String managementItemCode,
                                                       @Param("collegeCode") String collegeCode);

    ManagementItemEvaluationScoreRow upsertManagementItemEvaluationScore(@Param("request") SaveManagementItemEvaluationScoreRequest request,
                                                                         @Param("updatedBy") Long updatedBy,
                                                                         @Param("requestId") String requestId);

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
