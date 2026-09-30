package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists lecture records and the shared validation/history facts used by their transaction. */
@Mapper
public interface LectureAchievementMapper extends EducationAchievementValidationPort, EducationAchievementStatusHistoryPort {
    /** Resolves the current faculty-achievement organization instead of accepting a client-provided data scope. */
    String findOrganizationCodeForUser(@Param("userId") Long userId);
    List<LectureAchievementData> findAll(@Param("criteria") LectureAchievementSearchCriteria criteria, @Param("userId") Long userId);
    long countAll(@Param("criteria") LectureAchievementSearchCriteria criteria, @Param("userId") Long userId);
    LectureAchievementData findById(@Param("achievementId") Long achievementId);
    void insert(LectureAchievementData row);
    void update(LectureAchievementData row);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("userId") Long userId, @Param("reason") String reason, @Param("requestId") String requestId);
    @Override boolean hasDataScope(Long userId, Long ownerUserId, String organizationCode, String evaluationUnitCode);
    @Override boolean hasActiveInputPeriod(String evaluationYear, String organizationCode, String evaluationUnitCode, LocalDateTime checkedAt);
    @Override boolean hasEvaluationResultLock(String evaluationYear, String organizationCode, String evaluationUnitCode);
    @Override boolean isWithinEvaluationDate(String evaluationYear, String organizationCode, LocalDate occurrenceDate);
    @Override void record(EducationAchievementStatusTransition transition);
}
