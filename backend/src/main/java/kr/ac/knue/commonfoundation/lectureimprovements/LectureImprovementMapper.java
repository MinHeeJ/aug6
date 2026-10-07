package kr.ac.knue.commonfoundation.lectureimprovements;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML persistence adapter; header generated keys precede detail and audit writes. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(@Param("search") LectureImprovementSearch search,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("search") LectureImprovementSearch search,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    LectureImprovementRow find(@Param("id") Long id);
    LectureImprovementRow lock(@Param("id") Long id);
    int visible(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    List<String> organizations(@Param("userId") Long userId, @Param("date") LocalDate date);
    int managementItems(@Param("code") String code);
    void insertHeader(Map<String, Object> values);
    void updateHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body,
            @Param("semesterCode") String semesterCode);
    void updateDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body,
            @Param("semesterCode") String semesterCode);
    void statusHistory(@Param("id") Long id, @Param("userId") Long userId);
    void history(@Param("id") Long id, @Param("action") String action, @Param("field") String field,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
