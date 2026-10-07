package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL adapter for scoped reads and atomic header/detail/history writes. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(@Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles, @Param("pageOffset") long pageOffset);
    long count(@Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("id") Long id, @Param("lock") boolean lock);
    int inScope(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    String organization(@Param("userId") Long userId);
    List<String> managementItems(@Param("year") String year, @Param("userId") Long userId);
    int validManagementItem(@Param("code") String code, @Param("year") String year,
            @Param("userId") Long userId);
    void insertHeader(Map<String, Object> command);
    void insertDetail(Map<String, Object> command);
    int updateHeader(Map<String, Object> command);
    void updateDetail(Map<String, Object> command);
    void insertStatus(Map<String, Object> command);
    void insertHistory(Map<String, Object> command);
}
