package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists employment-rate achievements and their mandatory lifecycle/audit side effects. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<EmploymentRateAchievementRow> list(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateAchievementRow findById(@Param("achievementId") Long achievementId);

    EmploymentRateAchievementRow findInScope(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    List<EmploymentRateAchievementRow> listForDownload(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateBulkJobRow findBulkJob(@Param("jobId") String jobId);

    List<EmploymentRateBulkJobItemRow> listBulkJobItems(@Param("jobId") String jobId);

    void insert(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy);

    void update(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy);

    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);

    void insertChangeHistory(
            @Param("targetBusiness") String targetBusiness,
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
