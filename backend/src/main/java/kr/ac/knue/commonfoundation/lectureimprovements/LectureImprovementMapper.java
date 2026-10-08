package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** PostgreSQL adapter for scoped reads, locked writes, and transactional full-image histories. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    LectureImprovementRow find(@Param("id") Long id);
    LectureImprovementRow lock(@Param("id") Long id);
    int canRead(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    String organization(@Param("userId") Long userId);
    List<String> academicYears();
    List<String> semesters();
    List<LectureImprovementInputOption> inputOptions(@Param("userId") Long userId);
    int allowedItem(@Param("userId") Long userId, @Param("year") String year, @Param("code") String code);
    List<Long> lockFinalizations(@Param("userId") Long userId, @Param("year") String year);
    void insertHeader(Map<String, Object> values);
    void insertDetail(Map<String, Object> values);
    int updateHeader(Map<String, Object> values);
    void updateDetail(Map<String, Object> values);
    void history(Map<String, Object> values);
    void statusHistory(Map<String, Object> values);
}
