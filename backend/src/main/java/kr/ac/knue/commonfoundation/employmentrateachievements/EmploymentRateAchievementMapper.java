package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML persistence adapter for the V65 common header and read-only bulk results. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(Map<String, Object> scope);
    long count(Map<String, Object> scope);
    Map<String, Object> find(@Param("id") Long id, @Param("lock") boolean lock);
    List<String> organizations(@Param("teacher") Long teacher, @Param("date") LocalDate date);
    int countActiveItems(@Param("code") String code);
    int countMappedOrganizations(@Param("userId") Long userId);
    int insert(Map<String, Object> row);
    int update(Map<String, Object> row);
    int history(Map<String, Object> row);
    /** Records the initial DRAFT transition in the same transaction as creation. */
    void statusHistory(Map<String, Object> row);
    Map<String, Object> job(@Param("id") String id);
    List<Map<String, Object>> jobItems(@Param("id") String id);
}
