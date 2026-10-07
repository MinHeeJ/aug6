package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed access to the integrated employment ledger and existing platform metadata. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(@Param("query") Map<String, Object> query);
    long count(@Param("query") Map<String, Object> query);
    Map<String, Object> find(@Param("id") Long id, @Param("lock") boolean lock);
    int visible(@Param("query") Map<String, Object> query, @Param("id") Long id);
    List<String> organizations(@Param("userId") Long userId);
    List<String> years(@Param("organization") String organization);
    List<Map<String, Object>> managementItems(@Param("year") String year);
    int departmentScope(@Param("userId") Long userId, @Param("organization") String organization);
    Long teacher(@Param("employeeNo") String employeeNo);
    int duplicate(@Param("row") Map<String, Object> row);
    int insert(@Param("row") Map<String, Object> row);
    int update(@Param("row") Map<String, Object> row);
    void audit(@Param("entry") Map<String, Object> entry);
    void initialStatus(@Param("row") Map<String, Object> row);
    Map<String, Object> job(@Param("id") Long id);
    List<Map<String, Object>> jobItems(@Param("id") Long id);
    Map<String, Object> file(@Param("token") String token);
    List<String> templateColumns();
    String templateId();
    void upload(@Param("entry") Map<String, Object> entry);
    void uploadStatus(@Param("entry") Map<String, Object> entry);
    void clearStaging(@Param("uploadId") String uploadId);
    void staging(@Param("entry") Map<String, Object> entry);
    void uploadError(@Param("entry") Map<String, Object> entry);
    void history(@Param("entry") Map<String, Object> entry);
    void errorFile(@Param("entry") Map<String, Object> entry);
}
