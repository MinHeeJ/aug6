package kr.ac.knue.commonfoundation.lectureimprovement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for lecture-improvement source data and audit history writes. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> listLectureImprovements(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long countLectureImprovements(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    LectureImprovementRow findLectureImprovement(@Param("achievementId") Long achievementId);

    LectureImprovementRow findAccessibleLectureImprovement(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    LectureImprovementRow findLectureImprovementByManagementNo(@Param("managementNo") String managementNo);

    void insertLectureImprovement(
            @Param("managementNo") String managementNo,
            @Param("targetUserId") Long targetUserId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);

    void updateLectureImprovement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementContent") String achievementContent,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

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
