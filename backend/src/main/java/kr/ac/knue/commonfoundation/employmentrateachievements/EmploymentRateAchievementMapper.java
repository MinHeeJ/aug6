package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed FR-032 persistence; list and count share identical union-of-role predicates. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(@Param("q") Map<String, Object> criteria);
    long count(@Param("q") Map<String, Object> criteria);
    Map<String, Object> find(@Param("id") long id, @Param("lock") boolean lock);
    String organization(@Param("userId") long userId);
    void insert(Map<String, Object> row);
    int update(Map<String, Object> row);
    void history(@Param("id") long id, @Param("before") String before, @Param("after") String after,
            @Param("userId") long userId, @Param("requestId") String requestId, @Param("action") String action);
    void statusHistory(@Param("id") long id, @Param("userId") long userId, @Param("requestId") String requestId);
    Map<String, Object> job(@Param("id") String id, @Param("userId") long userId);
    List<Map<String, Object>> jobItems(@Param("id") String id);
}
