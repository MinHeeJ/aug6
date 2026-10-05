package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis persistence boundary for lecture-improvement headers, details, and
 * their mandatory audit records.
 */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("limit") int limit,
            @Param("offset") int offset);

    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    LectureImprovementRow findById(@Param("achievementId") Long achievementId);

    Long findIdByManagementNo(@Param("managementNo") String managementNo);


    int countReadableBy(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    void insertAchievement(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semesterCode") String semesterCode,
            @Param("createdBy") Long createdBy);

    void updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semesterCode") String semesterCode,
            @Param("updatedBy") Long updatedBy);

    void insertInitialStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("processedBy") Long processedBy);

    void insertChangeHistory(
            @Param("targetBusiness") String targetBusiness,
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);
}
