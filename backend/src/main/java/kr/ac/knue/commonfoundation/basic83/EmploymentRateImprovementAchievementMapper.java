package kr.ac.knue.commonfoundation.basic83;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for 취업률 제고 rows and mandatory lifecycle/audit side effects. */
@Mapper
public interface EmploymentRateImprovementAchievementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateImprovementRow findById(@Param("achievementId") Long achievementId);

    EmploymentRateImprovementRow findInScope(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateImprovementRow findByManagementNo(@Param("managementNo") String managementNo);

    void insert(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy);

    void update(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
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
