package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** FR-030 MyBatis adapter; all statements live in its paired XML mapper. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);

    long count(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);

    CourseOperationRow find(@Param("id") Long id);

    CourseOperationRow lock(@Param("id") Long id);

    int canRead(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);

    Long lockOwner(@Param("userId") Long userId);

    List<Long> lockFinalizations(@Param("userId") Long userId, @Param("year") String year);

    List<Long> lockInputPeriods(@Param("userId") Long userId, @Param("year") String year);

    String organization(@Param("userId") Long userId);

    List<CourseOperationManagementRule> managementRules(
            @Param("code") String code, @Param("year") String year);

    /** Generated achievementId is returned into this mutable command, before inserting its detail. */
    int insertHeader(Map<String, Object> command);

    int insertDetail(@Param("id") Long id, @Param("performanceDetails") String performanceDetails);

    int updateHeader(
            @Param("id") Long id,
            @Param("code") String code,
            @Param("date") LocalDate date,
            @Param("detailJson") String detailJson,
            @Param("attachmentsJson") String attachmentsJson,
            @Param("userId") Long userId);

    int updateDetail(@Param("id") Long id, @Param("performanceDetails") String performanceDetails);

    /** Full physical header/detail snapshot for audit, not just a UI projection. */
    String snapshot(@Param("id") Long id);

    int insertHistory(
            @Param("id") Long id,
            @Param("action") String action,
            @Param("before") String before,
            @Param("after") String after,
            @Param("userId") Long userId,
            @Param("requestId") String requestId);

    int insertInitialStatus(@Param("id") Long id, @Param("userId") Long userId);
}
