package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed header/detail persistence with generated-key insertion and atomic audit writes. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    long count(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    LectureImprovementRow find(@Param("id") Long id);
    LectureImprovementRow lock(@Param("id") Long id);
    String organization(@Param("userId") Long userId);
    int activeManagementItems(@Param("code") String code);
    void insertHeader(Map<String, Object> values);
    void insertDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body,
            @Param("userId") Long userId);
    int updateHeader(@Param("id") Long id, @Param("body") LectureImprovementRequest body,
            @Param("attachment") String attachment, @Param("userId") Long userId,
            @Param("requestId") String requestId);
    void updateDetail(@Param("id") Long id, @Param("body") LectureImprovementRequest body,
            @Param("userId") Long userId);
    void statusHistory(@Param("id") Long id, @Param("userId") Long userId,
            @Param("requestId") String requestId);
    void changeHistory(@Param("id") Long id, @Param("changeType") String changeType,
            @Param("before") String before, @Param("after") String after,
            @Param("userId") Long userId, @Param("requestId") String requestId);
}
