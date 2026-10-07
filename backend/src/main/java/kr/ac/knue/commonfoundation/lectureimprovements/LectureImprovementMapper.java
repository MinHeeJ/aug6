package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed adapter for the existing common header, feature detail and atomic histories. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    LectureImprovementRow find(@Param("id") Long id, @Param("lock") boolean lock);
    List<String> organizations(@Param("userId") Long userId);
    List<String> evaluationYears(@Param("organizationCode") String organizationCode);
    List<LectureImprovementManagementItem> managementItems();
    void insertHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body);
    void updateHeader(@Param("id") Long id, @Param("body") LectureImprovementRequest body,
            @Param("attachments") String attachments, @Param("userId") Long userId);
    void updateDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body);
    void statusHistory(@Param("id") Long id, @Param("userId") Long userId, @Param("requestId") String requestId);
    void changeHistory(@Param("id") Long id, @Param("changeType") String changeType,
            @Param("field") String field, @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
