package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for employment-rate-improvement rows and mandatory audit history. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    EmploymentRateImprovementRow findById(@Param("achievementId") Long achievementId);

    boolean isVisible(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    Long insertAchievement(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod);

    int updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

    int updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod);

    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);
}
