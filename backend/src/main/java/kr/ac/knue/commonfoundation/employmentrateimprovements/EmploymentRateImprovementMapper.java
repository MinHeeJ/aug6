package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed header/detail access; all writes participate in the service transaction. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow find(@Param("id") Long id);
    EmploymentRateImprovementRow lock(@Param("id") Long id);
    int visible(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    List<String> organizations(@Param("userId") Long userId, @Param("date") LocalDate date);
    int managementItems(@Param("code") String code);
    void insertHeader(Map<String, Object> header);
    void insertDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    int updateHeader(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body,
            @Param("attachmentRef") String attachmentRef, @Param("userId") Long userId);
    int updateDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    void statusHistory(@Param("id") Long id, @Param("userId") Long userId);
    void history(@Param("id") Long id, @Param("type") String type, @Param("field") String field,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
