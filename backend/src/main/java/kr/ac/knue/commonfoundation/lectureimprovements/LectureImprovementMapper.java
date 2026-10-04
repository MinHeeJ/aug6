package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists lecture-improvement headers, details, and their mandatory audit trail. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    LectureImprovementRow findVisible(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    Long findIdByManagementNo(@Param("managementNo") String managementNo);

    LectureImprovementRow findByManagementNo(@Param("managementNo") String managementNo);

    String findActiveOrganizationCode(@Param("userId") Long userId);

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
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester,
            @Param("createdBy") Long createdBy);

    void updateHeader(
            @Param("achievementId") Long achievementId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy);

    /**
     * Fails fast when a header update omits the mandatory audit actor.
     * Persistence updates must use the six-argument overload.
     */
    default void updateHeader(
            Long achievementId,
            String evaluationYear,
            String managementItemCode,
            LocalDate achievementDate,
            String attachmentRef) {
        throw new UnsupportedOperationException("Header updates require an updatedBy audit actor.");
    }

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester,
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
