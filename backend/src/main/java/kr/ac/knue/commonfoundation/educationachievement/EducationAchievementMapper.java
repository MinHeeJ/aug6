package kr.ac.knue.commonfoundation.educationachievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Persists education-area masters, degree details, and append-only status/change histories.
 */
@Mapper
public interface EducationAchievementMapper {
    List<EducationAchievementRow> list(@Param("achievementType") String achievementType, @Param("facultyUserId") Long facultyUserId,
            @Param("limit") int limit, @Param("offset") int offset);

    long count(@Param("achievementType") String achievementType, @Param("facultyUserId") Long facultyUserId);

    EducationAchievementRow findById(@Param("achievementId") Long achievementId);

    EducationAchievementRow insert(@Param("request") EducationAchievementSaveRequest request, @Param("facultyUserId") Long facultyUserId,
            @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);

    /** Updates only the common fields; details are replaced in the same service transaction. */
    int update(@Param("request") EducationAchievementSaveRequest request, @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);

    List<DegreeCompletionDetail> findDegreeCompletionDetails(@Param("achievementId") Long achievementId);

    void deleteDegreeCompletionDetails(@Param("achievementId") Long achievementId);

    void insertDegreeCompletionDetail(@Param("achievementId") Long achievementId, @Param("detail") DegreeCompletionDetail detail,
            @Param("changedBy") Long changedBy);

    List<StudentGuidanceDetail> findStudentGuidanceDetails(@Param("achievementId") Long achievementId);

    void deleteStudentGuidanceDetails(@Param("achievementId") Long achievementId);

    void insertStudentGuidanceDetail(@Param("achievementId") Long achievementId, @Param("detail") StudentGuidanceDetail detail,
            @Param("changedBy") Long changedBy);

    int hasActiveEducationInputPeriod();

    int updateStatus(@Param("achievementId") Long achievementId, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("changedBy") Long changedBy);

    /** Marks an achievement as deleted while retaining its master and audit columns. */
    int logicalDelete(@Param("achievementId") Long achievementId, @Param("changedBy") Long changedBy,
            @Param("deleteReason") String deleteReason);

    int allowedTransition(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus, @Param("roleCode") String roleCode);

    void insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason, @Param("requestId") String requestId);

    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);
}
