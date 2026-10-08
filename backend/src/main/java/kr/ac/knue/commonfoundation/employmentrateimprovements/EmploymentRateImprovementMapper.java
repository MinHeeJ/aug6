package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL adapter for scoped reads, ordered writes, lock checks and atomic histories. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") EmploymentRateImprovementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow find(@Param("id") Long id);
    EmploymentRateImprovementRow lock(@Param("id") Long id);
    Long lockTeacher(@Param("id") Long id);
    List<Long> lockFinalizations(@Param("teacherId") Long teacherId, @Param("year") String year);
    int countManagementItem(@Param("code") String code, @Param("year") String year);
    int countDuplicate(@Param("write") EmploymentRateImprovementWrite write);
    int insertHeader(@Param("write") EmploymentRateImprovementWrite write);
    int insertDetail(@Param("write") EmploymentRateImprovementWrite write);
    int updateHeader(@Param("write") EmploymentRateImprovementWrite write);
    int updateDetail(@Param("write") EmploymentRateImprovementWrite write);
    void insertStatusHistory(@Param("id") Long id, @Param("actor") Long actor, @Param("requestId") String requestId);
    void insertChangeHistory(
            @Param("id") Long id, @Param("changeType") String changeType, @Param("field") String field,
            @Param("before") String before, @Param("after") String after,
            @Param("actor") Long actor, @Param("requestId") String requestId);
}
