package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for 취업률 제고 rows, scoped reads, and their audit side effects. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateImprovementRow findById(@Param("achievementId") Long achievementId);

    EmploymentRateImprovementRow findByManagementNo(@Param("managementNo") String managementNo);

    String findAttachmentRefsById(@Param("achievementId") Long achievementId);

    long countAccessible(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    void insert(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("request") EmploymentRateImprovementRequest request,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("createdBy") Long createdBy);

    void update(
            @Param("achievementId") Long achievementId,
            @Param("request") EmploymentRateImprovementRequest request,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("updatedBy") Long updatedBy);

    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);

    void insertChangeHistory(
            @Param("targetBusiness") String targetBusiness,
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
}
