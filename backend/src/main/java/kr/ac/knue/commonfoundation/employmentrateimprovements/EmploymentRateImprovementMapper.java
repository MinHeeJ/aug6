package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists the common header and resource detail without crossing feature ownership. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<Map<String, Object>> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    Map<String, Object> find(@Param("id") Long id, @Param("lock") boolean lock);
    int inScope(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    List<String> organizations(@Param("userId") Long userId);
    List<String> years(@Param("organizationCode") String organizationCode);
    List<Map<String, Object>> managementItems(@Param("year") String year);
    int insertHeader(Map<String, Object> row);
    int insertDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    int updateHeader(
            @Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body,
            @Param("attachments") String attachments, @Param("actor") Long actor);
    int updateDetail(@Param("id") Long id, @Param("body") EmploymentRateImprovementRequest body);
    int audit(
            @Param("id") Long id, @Param("kind") String kind, @Param("field") String field,
            @Param("before") String before, @Param("after") String after,
            @Param("actor") Long actor, @Param("requestId") String requestId);
    int initialStatus(@Param("id") Long id, @Param("actor") Long actor, @Param("requestId") String requestId);
}
