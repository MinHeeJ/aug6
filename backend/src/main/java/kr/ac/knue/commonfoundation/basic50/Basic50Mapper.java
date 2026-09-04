package kr.ac.knue.commonfoundation.basic50;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface Basic50Mapper {
    List<BusinessSettingRow> listAppealBusinessSettings(@Param("criteria") BusinessSettingCriteria criteria);
    long countAppealBusinessSettings(@Param("criteria") BusinessSettingCriteria criteria);
    BusinessSettingRow findAppealBusinessSettingById(@Param("settingId") Long settingId);
    BusinessSettingRow insertAppealBusinessSetting(@Param("request") BusinessSettingSaveRequest request, @Param("userId") Long userId);
    BusinessSettingRow updateAppealBusinessSetting(@Param("request") BusinessSettingSaveRequest request, @Param("userId") Long userId);

    List<BusinessSettingRow> listResultViewBusinessSettings(@Param("criteria") BusinessSettingCriteria criteria);
    long countResultViewBusinessSettings(@Param("criteria") BusinessSettingCriteria criteria);
    BusinessSettingRow findResultViewBusinessSettingById(@Param("settingId") Long settingId);
    BusinessSettingRow insertResultViewBusinessSetting(@Param("request") BusinessSettingSaveRequest request, @Param("userId") Long userId);
    BusinessSettingRow updateResultViewBusinessSetting(@Param("request") BusinessSettingSaveRequest request, @Param("userId") Long userId);

    List<AuthorityRow> listCollegeEvaluationUnitAuthorities(@Param("criteria") BusinessSettingCriteria criteria);
    long countCollegeEvaluationUnitAuthorities(@Param("criteria") BusinessSettingCriteria criteria);
    AuthorityRow findCollegeEvaluationUnitAuthorityById(@Param("authorityId") Long authorityId);
    AuthorityRow insertCollegeEvaluationUnitAuthority(@Param("request") CollegeEvaluationUnitAuthoritySaveRequest request, @Param("userId") Long userId);
    AuthorityRow updateCollegeEvaluationUnitAuthority(@Param("request") CollegeEvaluationUnitAuthoritySaveRequest request, @Param("userId") Long userId);

    List<ResearchCriterionRow> listResearchClassificationCriteria(@Param("criteria") ResearchCriterionCriteria criteria);
    long countResearchClassificationCriteria(@Param("criteria") ResearchCriterionCriteria criteria);
    ResearchCriterionRow findResearchCriterionById(@Param("criterionId") Long criterionId);
    ResearchCriterionRow insertResearchCriterion(@Param("request") ResearchCriterionSaveRequest request, @Param("userId") Long userId);
    ResearchCriterionRow updateResearchCriterion(@Param("request") ResearchCriterionSaveRequest request, @Param("userId") Long userId);

    List<ResearchAchievementRow> listUnconfirmedResearchAchievements(@Param("criteria") ResearchAchievementCriteria criteria);
    long countUnconfirmedResearchAchievements(@Param("criteria") ResearchAchievementCriteria criteria);
    ResearchAchievementRow findResearchAchievementById(@Param("achievementId") Long achievementId);
    ResearchAchievementRow confirmResearchAchievement(@Param("achievementId") Long achievementId, @Param("managementCriterionCode") String managementCriterionCode, @Param("userId") Long userId);

    List<PersonalScoreItem> listPersonalScoreItems(@Param("teacherUserId") Long teacherUserId, @Param("evaluationYear") String evaluationYear, @Param("areaCode") String areaCode);
    String findUserName(@Param("userId") Long userId);
    int countConfirmedEvaluationLocks(@Param("evaluationYear") String evaluationYear, @Param("organizationCode") String organizationCode, @Param("evaluationUnitCode") String evaluationUnitCode);
    int existsAuthorizedEvaluationOrganization(@Param("userId") Long userId, @Param("organizationCode") String organizationCode);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey, @Param("changeType") String changeType, @Param("fieldName") String fieldName, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason, @Param("requestId") String requestId);
    void insertSensitiveAccessLog(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") String targetId, @Param("accessReason") String accessReason, @Param("requestId") String requestId);
}
