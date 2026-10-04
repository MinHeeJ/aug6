package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for shared BASIC-83 headers and course-operation details. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    CourseOperationRow findScoped(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    String findActiveOrganizationCode(@Param("userId") Long userId);

    CourseOperationRow findByManagementNo(@Param("managementNo") String managementNo);

    void insertHeader(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails,
            @Param("createdBy") Long createdBy);

    void updateHeader(
            @Param("achievementId") Long achievementId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails,
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
