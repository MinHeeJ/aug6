package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed FR-029 ledger/detail persistence and atomic history adapter. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    long count(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    EmploymentRateImprovementRow find(@Param("id") Long id);
    EmploymentRateImprovementRow lock(@Param("id") Long id);
    int visible(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    List<String> organizations(@Param("userId") Long userId);
    List<Map<String, Object>> managementItems(@Param("code") String code, @Param("year") String year);
    List<ManagementItemOption> availableManagementItems();
    void insertHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    int updateHeader(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body,
            @Param("userId") Long userId);
    void updateDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void statusHistory(@Param("id") Long id, @Param("userId") Long userId);
    void changeHistory(@Param("id") Long id, @Param("action") String action,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
