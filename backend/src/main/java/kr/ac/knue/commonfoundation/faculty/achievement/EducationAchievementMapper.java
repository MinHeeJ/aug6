package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Persists and reads the education-area achievement sources while keeping the
 * authorization, period, finalization, and audit facts in the same adapter.
 */
@Mapper
public interface EducationAchievementMapper {
    List<LectureEvaluationAchievementRow> listLectureEvaluationAchievements(
            @Param("criteria") LectureEvaluationAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("restrictToSelf") boolean restrictToSelf,
            @Param("unrestricted") boolean unrestricted
    );

    long countLectureEvaluationAchievements(
            @Param("criteria") LectureEvaluationAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("restrictToSelf") boolean restrictToSelf,
            @Param("unrestricted") boolean unrestricted
    );

    int isAchievementDataScopeAllowed(
            @Param("actorUserId") Long actorUserId,
            @Param("targetUserId") Long targetUserId
    );

    int hasActiveInputPeriod(@Param("evaluationYear") String evaluationYear);

    int isEvaluationConfirmed(
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear
    );

    EducationAchievementEvaluationPeriod findEvaluationPeriod(
            @Param("evaluationYear") String evaluationYear
    );

    int existsActiveManagementItem(
            @Param("managementItemCode") String managementItemCode,
            @Param("evaluationYear") String evaluationYear
    );

    LectureEvaluationAchievementRow insertLectureEvaluationAchievement(
            @Param("evaluationYear") String evaluationYear,
            @Param("targetUserId") Long targetUserId,
            @Param("managementItemCode") String managementItemCode,
            @Param("occurredDate") LocalDate occurredDate,
            @Param("achievementDetailJson") String achievementDetailJson,
            @Param("createdBy") Long createdBy
    );

    void insertEducationAchievementStatusHistory(
            @Param("history") EducationAchievementGuardChain.EducationAchievementStatusHistory history,
            @Param("createdBy") Long createdBy
    );

    void insertChangeHistory(
            @Param("targetBusiness") String targetBusiness,
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason,
            @Param("requestId") String requestId
    );
}
