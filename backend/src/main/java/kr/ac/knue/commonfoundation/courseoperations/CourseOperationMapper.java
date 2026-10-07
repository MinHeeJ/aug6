package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML persistence boundary for scoped reads and atomic header/detail/history writes. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(@Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("id") Long id);
    CourseOperationRow lock(@Param("id") Long id);
    int visible(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    List<String> organizations(@Param("userId") Long userId);
    List<String> years(@Param("organization") String organization);
    List<CourseOperationManagementItem> managementItems();
    void insertHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("details") String details);
    void updateHeader(Map<String, Object> values);
    void updateDetail(@Param("id") Long id, @Param("details") String details);
    void initialStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("requestId") String requestId);
    void history(@Param("id") Long id, @Param("changeType") String changeType,
            @Param("field") String field, @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
