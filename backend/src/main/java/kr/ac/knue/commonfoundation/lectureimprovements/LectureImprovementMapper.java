package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed header/detail persistence and transactional lifecycle/snapshot audit writes. */
@Mapper
public interface LectureImprovementMapper {
    List<Map<String, Object>> list(@Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    long count(@Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("userId") Long userId, @Param("roles") List<String> roles);
    Map<String, Object> find(@Param("id") Long id, @Param("lock") boolean lock);
    List<Map<String, Object>> managementItems(@Param("userId") Long userId,
            @Param("year") String year);
    String organization(@Param("userId") Long userId);
    void insertHeader(Map<String, Object> values);
    void insertDetail(Map<String, Object> values);
    int updateHeader(Map<String, Object> values);
    void updateDetail(Map<String, Object> values);
    void insertStatus(Map<String, Object> values);
    void insertHistory(Map<String, Object> values);
}
