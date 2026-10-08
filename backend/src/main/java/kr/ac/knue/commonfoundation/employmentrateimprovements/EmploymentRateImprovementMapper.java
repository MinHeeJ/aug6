package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML adapter for header/detail, scoped reads, row locks and transactional histories. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow find(@Param("id") Long id, @Param("lock") boolean lock);
    String organization(@Param("userId") Long userId);
    List<String> managementItems(@Param("userId") Long userId, @Param("year") String year);
    int allowedItem(@Param("userId") Long userId, @Param("year") String year, @Param("code") String code);
    Long lockTeacher(@Param("userId") Long userId);
    List<Long> lockFinalizations(@Param("userId") Long userId, @Param("year") String year);
    void insertHeader(Map<String, Object> values);
    int updateHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void updateDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void statusHistory(@Param("id") Long id, @Param("actor") Long actor, @Param("trace") String trace);
    void changeHistory(@Param("id") Long id, @Param("action") String action,
            @Param("field") String field, @Param("before") String before, @Param("after") String after,
            @Param("actor") Long actor, @Param("trace") String trace);
}
