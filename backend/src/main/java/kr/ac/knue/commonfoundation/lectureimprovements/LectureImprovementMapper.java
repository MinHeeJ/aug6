package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists lecture-improvement source/detail rows and required audit side effects. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    LectureImprovementRow findScoped(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    LectureImprovementRow findByManagementNo(@Param("managementNo") String managementNo);

    String findOrganizationCode(@Param("teacherUserId") Long teacherUserId);

    void insertAchievement(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("createdBy") Long createdBy);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semesterCode") String semesterCode,
            @Param("createdBy") Long createdBy);

    int updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("updatedBy") Long updatedBy);

    int updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semesterCode") String semesterCode,
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
            @Param("requestId") String requestId,
            @Param("changedAt") LocalDateTime changedAt);
}
