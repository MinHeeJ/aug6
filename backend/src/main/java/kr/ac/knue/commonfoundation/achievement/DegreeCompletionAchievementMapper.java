package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for degree-completion headers, student details, and audit history. */
@Mapper
public interface DegreeCompletionAchievementMapper {
    List<DegreeCompletionAchievementHeaderRow> list(
            @Param("criteria") DegreeCompletionAchievementSearchCriteria criteria
    );

    long count(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria);

    DegreeCompletionAchievementHeaderRow findById(@Param("achievementId") Long achievementId);

    DegreeCompletionAchievementHeaderRow findByManagementNo(@Param("managementNo") String managementNo);

    List<DegreeCompletionStudentRow> findStudents(@Param("achievementId") Long achievementId);

    String findActiveOrganizationCode(@Param("targetUserId") Long targetUserId);

    int insertAchievement(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("organizationCode") String organizationCode,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("createdBy") Long createdBy,
            @Param("changeReason") String changeReason
    );

    int updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetail") String achievementDetail,
            @Param("attachmentRef") String attachmentRef,
            @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason
    );

    int deleteStudents(@Param("achievementId") Long achievementId);

    int insertStudent(
            @Param("achievementId") Long achievementId,
            @Param("student") DegreeCompletionStudentRequest student,
            @Param("userId") Long userId
    );

    int insertChangeHistory(
            @Param("achievementId") Long achievementId,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason
    );
}
