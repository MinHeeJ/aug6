package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL adapter for scoped reads, generated-key header writes, details, and atomic histories. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("id") Long id, @Param("lock") boolean lock);
    int countScope(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    String findOrganization(@Param("userId") Long userId);
    List<CourseOperationManagementItem> managementItems();
    int countManagementItem(@Param("code") String code);
    int countAttachment(@Param("token") String token, @Param("userId") Long userId);
    void insertHeader(Map<String, Object> header);
    int updateHeader(@Param("id") Long id, @Param("body") CourseOperationRequest body,
            @Param("status") String status, @Param("attachments") String attachments, @Param("userId") Long userId);
    void insertDetail(@Param("id") Long id, @Param("details") String details);
    void updateDetail(@Param("id") Long id, @Param("details") String details);
    void insertStatusHistory(@Param("id") Long id, @Param("before") String before,
            @Param("after") String after, @Param("userId") Long userId, @Param("requestId") String requestId);
    void insertChangeHistory(@Param("id") Long id, @Param("action") String action,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
