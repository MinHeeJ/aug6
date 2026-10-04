package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists lecture-improvement headers, details, and their required immutable audit records. */
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

    LectureImprovementRow findVisible(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    void insertAchievement(@Param("command") LectureImprovementInsertCommand command);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semesterCode") Integer semesterCode);

    void updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semesterCode") Integer semesterCode);

    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);

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
