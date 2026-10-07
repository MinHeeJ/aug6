package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed queries for FR-029; header, detail and history writes share the service transaction. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementStoredRow> list(
            @Param("criteria") EmploymentRateImprovementCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") EmploymentRateImprovementCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);

    EmploymentRateImprovementStoredRow find(@Param("id") Long id, @Param("lock") boolean lock);

    int countScope(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);

    String organization(@Param("userId") Long userId);

    int countManagementItem(@Param("code") String code);

    /** Uses JDBC generated keys on a mutable parameter map, before inserting the dependent detail. */
    int insertHeader(Map<String, Object> values);

    int insertDetail(Map<String, Object> values);

    int updateHeader(Map<String, Object> values);

    int updateDetail(Map<String, Object> values);

    int insertStatusHistory(Map<String, Object> values);

    int insertChangeHistory(Map<String, Object> values);
}
