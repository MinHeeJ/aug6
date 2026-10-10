package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/** SQL adapter for the normalized ledger and the existing Excel diagnostics tables. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(Map<String, Object> parameters);
    long count(Map<String, Object> parameters);
    Map<String, Object> find(Map<String, Object> parameters);
    void insert(Map<String, Object> row);
    int update(Map<String, Object> row);
    void history(Map<String, Object> values);
    void statusHistory(Map<String, Object> values);
    List<Map<String, Object>> managementItems();
    String organization(Long userId);
    Long teacher(String employeeNo);
    int scope(Map<String, Object> values);
    int function(Map<String, Object> values);
    int managementItem(Map<String, Object> values);
    int attachment(Map<String, Object> values);
    int duplicate(Map<String, Object> values);
    Map<String, Object> job(String jobId);
    List<Map<String, Object>> jobItems(String jobId);
    String template();
    List<String> templateColumns(String templateId);
    void upload(Map<String, Object> values);
    void staging(Map<String, Object> values);
    void error(Map<String, Object> values);
    void uploadHistory(Map<String, Object> values);
    Map<String, Object> findUpload(Map<String, Object> values);
    List<Map<String, Object>> stagingRows(String uploadId);
    List<Map<String, Object>> errors(String uploadId);
    List<Map<String, Object>> histories(Map<String, Object> values);
    void committed(String uploadId);
    void savedCount(Map<String, Object> values);
}
