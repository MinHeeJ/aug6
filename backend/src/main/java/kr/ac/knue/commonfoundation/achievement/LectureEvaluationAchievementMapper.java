package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists lecture-evaluation records and the shared validation/history facts used by their transaction. */
@Mapper
public interface LectureEvaluationAchievementMapper extends EducationAchievementValidationPort, EducationAchievementStatusHistoryPort {
    String findOrganizationCodeForUser(@Param("userId") Long userId);
    List<LectureEvaluationAchievementData> findAll(@Param("criteria") LectureEvaluationAchievementSearchCriteria criteria, @Param("userId") Long userId);
    long countAll(@Param("criteria") LectureEvaluationAchievementSearchCriteria criteria, @Param("userId") Long userId);
    LectureEvaluationAchievementData findById(@Param("achievementId") Long achievementId);
    void insert(LectureEvaluationAchievementData row);
    void update(LectureEvaluationAchievementData row);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("userId") Long userId, @Param("reason") String reason, @Param("requestId") String requestId);
    @Override boolean hasDataScope(Long userId, Long ownerUserId, String organizationCode, String evaluationUnitCode);
    @Override boolean hasActiveInputPeriod(String evaluationYear, String organizationCode, String evaluationUnitCode, LocalDateTime checkedAt);
    @Override boolean hasEvaluationResultLock(String evaluationYear, String organizationCode, String evaluationUnitCode);
    @Override boolean isWithinEvaluationDate(String evaluationYear, String organizationCode, LocalDate occurrenceDate);
    @Override void record(EducationAchievementStatusTransition transition);
}
