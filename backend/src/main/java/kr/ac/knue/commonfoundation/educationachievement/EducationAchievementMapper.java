package kr.ac.knue.commonfoundation.educationachievement;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * Provides the shared persistence boundary for all four education-achievement types.
 *
 * <p>Story-specific queries are added here rather than duplicating direct SQL in individual
 * controllers, preserving the common source table as the authoritative data boundary.</p>
 */
@Mapper
public interface EducationAchievementMapper {

    /**
     * Counts active records for a requested achievement type without treating a null filter as a
     * SQL predicate; callers must supply the selected type explicitly.
     *
     * @param achievementType one of the education achievement type codes
     * @return active source-row count
     */
    @Select("""
            SELECT COUNT(*)
            FROM education_achievements
            WHERE achievement_type = #{achievementType}
              AND deleted_yn = 'N'
            """)
    int countActiveByAchievementType(String achievementType);
}
