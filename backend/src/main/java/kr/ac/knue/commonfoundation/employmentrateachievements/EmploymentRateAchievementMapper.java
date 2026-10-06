package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** FR-032 adapter; XML shares predicates between list/count and binds every caller-supplied value. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(
            @Param("query") Map<String, Object> query,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    long count(
            @Param("query") Map<String, Object> query,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    Map<String, Object> find(@Param("id") Long id, @Param("lock") boolean lock);
    int visible(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    String organization(@Param("userId") Long userId);
    List<Map<String, Object>> itemRules(@Param("code") String code, @Param("year") String year);
    /** Freezes guard configuration and finalization inserts for the duration of a write transaction. */
    void lockMutationGuards();
    void insert(Map<String, Object> values);
    int update(Map<String, Object> values);
    void history(
            @Param("id") Long id,
            @Param("type") String type,
            @Param("before") String before,
            @Param("after") String after,
            @Param("actor") Long actor,
            @Param("requestId") String requestId);
    void initialStatus(@Param("id") Long id, @Param("actor") Long actor);
    Map<String, Object> job(@Param("id") String id, @Param("userId") Long userId);
    List<Map<String, Object>> jobItems(@Param("id") String id, @Param("userId") Long userId);
}
