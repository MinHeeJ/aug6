package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed persistence for FR-030, including generated keys and transactional audit writes. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles,
            @Param("pageOffset") long pageOffset);
    long count(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("id") Long id, @Param("lock") boolean lock);
    String organization(@Param("userId") Long userId);
    int countManagementItem(@Param("code") String code);
    /** Writes the header first and returns the generated achievementId in the command map. */
    void insertHeader(Map<String, Object> command);
    void insertDetail(Map<String, Object> command);
    int updateHeader(Map<String, Object> command);
    int updateDetail(Map<String, Object> command);
    void insertStatusHistory(Map<String, Object> command);
    void insertChangeHistory(Map<String, Object> command);
}
