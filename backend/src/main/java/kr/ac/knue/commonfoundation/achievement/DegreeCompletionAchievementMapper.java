package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for degree-completion headers, students, validation facts and audit records. */
@Mapper
public interface DegreeCompletionAchievementMapper extends EducationAchievementValidationPort, EducationAchievementStatusHistoryPort {
    String findOrganizationCodeForUser(@Param("userId") Long userId);
    List<DegreeCompletionAchievementData> findAll(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria,
            @Param("userId") Long userId);
    long countAll(@Param("criteria") DegreeCompletionAchievementSearchCriteria criteria, @Param("userId") Long userId);
    DegreeCompletionAchievementData findById(@Param("achievementId") Long achievementId);
    void insert(DegreeCompletionAchievementData row);
    void update(DegreeCompletionAchievementData row);
    void deleteStudents(@Param("achievementId") Long achievementId);
    void insertStudent(@Param("achievementId") Long achievementId,
            @Param("student") DegreeCompletionStudentRequest student, @Param("createdBy") Long createdBy);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("userId") Long userId, @Param("reason") String reason, @Param("requestId") String requestId);
    @Override boolean hasDataScope(Long userId, Long ownerUserId, String organizationCode, String evaluationUnitCode);
    @Override boolean hasActiveInputPeriod(String evaluationYear, String organizationCode, String evaluationUnitCode,
            LocalDateTime checkedAt);
    @Override boolean hasEvaluationResultLock(String evaluationYear, String organizationCode, String evaluationUnitCode);
    @Override boolean isWithinEvaluationDate(String evaluationYear, String organizationCode, LocalDate occurrenceDate);
    @Override void record(EducationAchievementStatusTransition transition);
}
