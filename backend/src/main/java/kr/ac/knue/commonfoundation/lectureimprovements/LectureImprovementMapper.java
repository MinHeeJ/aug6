package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists normalized lecture improvements and audit records in the owning service transaction. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(@Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    LectureImprovementRow find(@Param("id") Long id);
    LectureImprovementRow lock(@Param("id") Long id);
    Long lockTeacher(@Param("userId") Long userId);
    String organization(@Param("userId") Long userId);
    int validSemester(@Param("semester") String semester);
    int validManagementItem(@Param("code") String code, @Param("year") String year,
            @Param("organization") String organization);
    int insertHeader(Map<String, Object> command);
    int updateHeader(Map<String, Object> command);
    int insertDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body);
    int updateDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body);
    int insertStatusHistory(@Param("id") Long id, @Param("userId") Long userId,
            @Param("requestId") String requestId);
    int insertChangeHistory(@Param("id") Long id, @Param("action") String action,
            @Param("field") String field, @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
