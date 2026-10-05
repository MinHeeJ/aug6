package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for the BASIC-83 취업률 제고 source, detail, and audit writes. */
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

    EmploymentRateImprovementRow findByManagementNo(@Param("managementNo") String managementNo);

    void insertSource(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("createdBy") Long createdBy);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
            @Param("createdBy") Long createdBy);

    void updateSource(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentRefs") String attachmentRefs,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
            @Param("updatedBy") Long updatedBy);

    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("processedBy") Long processedBy);

    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("requestId") String requestId);
}
