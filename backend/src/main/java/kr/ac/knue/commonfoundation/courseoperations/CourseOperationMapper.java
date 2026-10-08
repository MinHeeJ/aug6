package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed normalized persistence, row locks, generated keys and transactional audit writes. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("achievementId") Long achievementId);
    CourseOperationRow lock(@Param("achievementId") Long achievementId);
    String organization(@Param("userId") Long userId);
    String activeYear(@Param("userId") Long userId);
    int editableItem(
            @Param("code") String code, @Param("year") String year,
            @Param("organization") String organization);
    int duplicate(
            @Param("userId") Long userId, @Param("year") String year,
            @Param("body") CourseOperationRequest body, @Param("excludeId") Long excludeId);
    void insertHeader(Map<String, Object> values);
    int updateHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("details") String details);
    void updateDetail(@Param("id") Long id, @Param("details") String details);
    void statusHistory(@Param("id") Long id, @Param("userId") Long userId, @Param("requestId") String requestId);
    void changeHistory(
            @Param("id") Long id, @Param("operation") String operation,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
