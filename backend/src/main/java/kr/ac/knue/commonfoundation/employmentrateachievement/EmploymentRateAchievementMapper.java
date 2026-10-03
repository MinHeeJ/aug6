package kr.ac.knue.commonfoundation.employmentrateachievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for employment-rate rows and role-derived read scopes. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<EmploymentRateAchievementRow> list(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("visibility") EmploymentRateAchievementVisibility visibility);

    long count(
            @Param("criteria") EmploymentRateAchievementSearchCriteria criteria,
            @Param("visibility") EmploymentRateAchievementVisibility visibility);

    EmploymentRateAchievementRow findAccessible(
            @Param("achievementId") Long achievementId,
            @Param("visibility") EmploymentRateAchievementVisibility visibility);
}
