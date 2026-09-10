package kr.ac.knue.commonfoundation.basic60;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface Basic60Mapper {
    List<OperationalSettingRow> listElementSettings(@Param("criteria") OperationalSettingSearchCriteria criteria);
    long countElementSettings(@Param("criteria") OperationalSettingSearchCriteria criteria);
    OperationalSettingRow findElementSettingByKey(@Param("request") SaveEvaluationElementManagementItemSettingRequest request);
    void upsertElementSetting(@Param("request") SaveEvaluationElementManagementItemSettingRequest request, @Param("updatedBy") Long updatedBy);

    List<OperationalSettingRow> listParticipationSettings(@Param("criteria") OperationalSettingSearchCriteria criteria);
    long countParticipationSettings(@Param("criteria") OperationalSettingSearchCriteria criteria);
    OperationalSettingRow findParticipationSettingByKey(@Param("request") SaveParticipationAllocationRateSettingRequest request);
    void upsertParticipationSetting(@Param("request") SaveParticipationAllocationRateSettingRequest request, @Param("updatedBy") Long updatedBy);

    List<OperationalSettingRow> listScoreSettings(@Param("criteria") OperationalSettingSearchCriteria criteria);
    long countScoreSettings(@Param("criteria") OperationalSettingSearchCriteria criteria);
    OperationalSettingRow findScoreSettingByKey(@Param("request") SaveManagementItemEvaluationScoreSettingRequest request);
    void upsertScoreSetting(@Param("request") SaveManagementItemEvaluationScoreSettingRequest request, @Param("updatedBy") Long updatedBy);

    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);
    Boolean hasConfirmedElementSettingImpact(@Param("request") SaveEvaluationElementManagementItemSettingRequest request);
    Boolean hasConfirmedParticipationSettingImpact(@Param("request") SaveParticipationAllocationRateSettingRequest request);
    Boolean hasConfirmedScoreSettingImpact(@Param("request") SaveManagementItemEvaluationScoreSettingRequest request);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness,
                             @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType,
                             @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue,
                             @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy,
                             @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);

    List<CourseAreaGroupGradeRow> listCourseAreaGroupGrades(@Param("criteria") CourseAreaGroupGradeSearchCriteria criteria);
    long countCourseAreaGroupGrades(@Param("criteria") CourseAreaGroupGradeSearchCriteria criteria);
    void insertGradeQueryAudit(@Param("targetKey") String targetKey, @Param("changedBy") Long changedBy, @Param("requestId") String requestId);
}
