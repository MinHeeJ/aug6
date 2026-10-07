package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML persistence adapter for the V65 header/detail and mandatory histories. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow find(@Param("id") Long id, @Param("lock") boolean lock);
    List<String> organizations(@Param("userId") Long userId);
    List<EmploymentRateImprovementManagementItem> managementItems(
            @Param("year") String year, @Param("organization") String organization);
    void lockOwner(@Param("userId") Long userId);
    List<Long> lockFinalizations(@Param("userId") Long userId, @Param("year") String year);
    void insertHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    int updateHeader(Map<String, Object> values);
    void updateDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void insertStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("requestId") String requestId);
    void insertHistory(@Param("id") Long id, @Param("type") String type,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
