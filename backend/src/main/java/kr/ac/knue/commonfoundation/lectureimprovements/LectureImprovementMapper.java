package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML persistence for lecture parent/detail rows, shared scope, options and atomic histories. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    LectureImprovementRow find(@Param("id") Long id, @Param("lock") boolean lock);
    int canRead(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    String organization(@Param("userId") Long userId);
    List<Map<String, Object>> semesters();
    List<Map<String, Object>> managementItems(@Param("year") String year);
    int validSemester(@Param("academicYear") String academicYear, @Param("semester") String semester);
    int validManagementItem(@Param("year") String year, @Param("code") String code);
    int validAttachment(@Param("token") String token, @Param("userId") Long userId);
    void insertHeader(@Param("row") Map<String, Object> row);
    int updateHeader(@Param("row") Map<String, Object> row);
    void insertDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body);
    void updateDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body);
    void history(@Param("id") Long id, @Param("before") String before, @Param("after") String after,
            @Param("action") String action, @Param("userId") Long userId, @Param("requestId") String requestId);
    void statusHistory(@Param("id") Long id, @Param("previous") String previous,
            @Param("next") String next, @Param("userId") Long userId, @Param("requestId") String requestId);
}
