package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Queries the persisted authorization, input-period, evaluation-period, and finalization facts
 * required before an education achievement mutation can proceed.
 */
@Mapper
public interface EducationAchievementAccessMapper {
    int countDepartmentScopedTarget(
            @Param("actorUserId") Long actorUserId,
            @Param("targetUserId") Long targetUserId,
            @Param("organizationCode") String organizationCode
    );

    int countActiveInputPeriods(
            @Param("evaluationYear") String evaluationYear,
            @Param("areaCode") String areaCode,
            @Param("organizationCode") String organizationCode,
            @Param("checkedAt") LocalDateTime checkedAt
    );

    int countEvaluationPeriodContainingDate(
            @Param("evaluationYear") String evaluationYear,
            @Param("areaCode") String areaCode,
            @Param("organizationCode") String organizationCode,
            @Param("occurredDate") LocalDate occurredDate
    );

    int countConfirmedFinalizations(
            @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear
    );
}
