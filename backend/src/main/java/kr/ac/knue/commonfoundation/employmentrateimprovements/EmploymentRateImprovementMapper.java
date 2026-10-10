package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML persistence adapter for scoped reads, generated-key writes and atomic audit records. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearch criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") EmploymentRateImprovementSearch criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow find(@Param("id") Long id, @Param("lock") boolean lock);
    int visible(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    String organization(@Param("userId") Long userId);
    List<String> managementItemCodes();
    int functionAllowed(@Param("roles") List<String> roles, @Param("function") String function);
    void insertHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void updateHeader(
            @Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body,
            @Param("status") String status, @Param("userId") Long userId);
    void updateDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void history(
            @Param("id") Long id, @Param("action") String action,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
    void statusHistory(
            @Param("id") Long id, @Param("previous") String previous, @Param("next") String next,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
