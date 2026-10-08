package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL adapter for normalized records, diagnostics and their transactional histories. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(@Param("q") Map<String, Object> query);
    long count(@Param("q") Map<String, Object> query);
    Map<String, Object> find(@Param("id") Long id, @Param("lock") boolean lock);
    Long insert(@Param("r") Map<String, Object> row);
    int update(@Param("r") Map<String, Object> row);
    String organization(@Param("id") Long id);
    Long employee(@Param("employeeNo") String employeeNo);
    List<Map<String, Object>> rules(@Param("code") String code, @Param("year") String year);
    int duplicates(@Param("r") Map<String, Object> row);
    void history(@Param("r") Map<String, Object> row, @Param("field") String field,
            @Param("before") String before, @Param("after") String after, @Param("action") String action);
    void statusHistory(@Param("r") Map<String, Object> row);
    Map<String, Object> job(@Param("id") String id);
    List<Map<String, Object>> jobItems(@Param("id") String id);
    Map<String, Object> template();
    List<String> templateColumns(@Param("id") String id);
    void upload(@Param("r") Map<String, Object> row);
    void stage(@Param("id") String id, @Param("number") int number, @Param("payload") String payload,
            @Param("status") String status);
    void error(@Param("id") String id, @Param("number") int number, @Param("reason") String reason);
    void uploadHistory(@Param("r") Map<String, Object> row);
    Map<String, Object> findUpload(@Param("id") String id, @Param("lock") boolean lock);
    List<String> staged(@Param("id") String id);
    List<Map<String, Object>> errors(@Param("id") String id);
    List<Map<String, Object>> histories(@Param("id") Long id, @Param("admin") boolean admin);
    void committed(@Param("id") String id, @Param("count") int count, @Param("requestId") String requestId);
}
