package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** PostgreSQL adapter for the atomic common-header/course-detail workflow. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(@Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("id") Long id, @Param("lock") boolean lock);
    String organization(@Param("userId") Long userId);
    List<CourseOperationItemOption> items(@Param("userId") Long userId);
    int allowedItem(@Param("userId") Long userId, @Param("year") String year, @Param("code") String code);
    List<Long> lockFinalizations(@Param("userId") Long userId, @Param("year") String year);
    void insertHeader(Map<String, Object> values);
    void insertDetail(Map<String, Object> values);
    int updateHeader(Map<String, Object> values);
    void updateDetail(Map<String, Object> values);
    void insertStatus(Map<String, Object> values);
    void insertHistory(@Param("id") Long id, @Param("changeType") String changeType,
            @Param("before") String before, @Param("after") String after,
            @Param("actor") Long actor, @Param("requestId") String requestId);
}
