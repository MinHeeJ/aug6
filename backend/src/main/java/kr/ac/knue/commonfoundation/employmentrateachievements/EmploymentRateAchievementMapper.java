package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/** XML persistence adapter for the foundation header, audit and Excel staging tables. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(Map<String, Object> parameters);
    long count(Map<String, Object> parameters);
    Map<String, Object> find(Map<String, Object> parameters);
    Map<String, Object> lock(Map<String, Object> parameters);
    String organization(Long userId);
    List<Map<String, Object>> inputOptions(Long userId);
    Map<String, Object> setting(Map<String, Object> parameters);
    List<Long> lockFinalizations(Map<String, Object> parameters);
    void insert(Map<String, Object> parameters);
    int update(Map<String, Object> parameters);
    void statusHistory(Map<String, Object> parameters);
    void audit(Map<String, Object> parameters);
    long duplicates(Map<String, Object> parameters);
    Map<String, Object> job(String jobId);
    List<Map<String, Object>> jobItems(String jobId);
    Map<String, Object> teacher(String employeeNo);
    Map<String, Object> template();
    List<String> columns(String templateId);
    void upload(Map<String, Object> parameters);
    void stage(Map<String, Object> parameters);
    void error(Map<String, Object> parameters);
    void history(Map<String, Object> parameters);
    Map<String, Object> uploadInfo(String uploadId);
    Map<String, Object> lockUpload(String uploadId);
    List<Map<String, Object>> stages(String uploadId);
    List<Map<String, Object>> errors(String uploadId);
    List<Map<String, Object>> histories(Map<String, Object> parameters);
    void committed(String uploadId);
}
