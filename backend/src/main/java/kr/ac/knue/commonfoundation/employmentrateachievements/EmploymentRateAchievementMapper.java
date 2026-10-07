package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/** PostgreSQL adapters for the shared education ledger and retained upload diagnostics. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<Map<String, Object>> list(Map<String, Object> query);
    long count(Map<String, Object> query);
    Map<String, Object> find(Map<String, Object> query);
    Map<String, Object> lock(Map<String, Object> query);
    void insert(Map<String, Object> row);
    int update(Map<String, Object> row);
    void history(Map<String, Object> change);
    void initialStatus(Map<String, Object> row);
    List<String> organizations(Long userId);
    int managementItemCount(Map<String, Object> query);
    List<Map<String, Object>> managementItems(Map<String, Object> query);
    int duplicate(Map<String, Object> row);
    Map<String, Object> job(Map<String, Object> query);
    List<Map<String, Object>> jobItems(String jobId);
    Map<String, Object> template();
    List<String> templateColumns(String templateId);
    Map<String, Object> employee(String employeeNo);
    int uploadScope(Map<String, Object> query);
    void insertUpload(Map<String, Object> row);
    void insertStaging(Map<String, Object> row);
    void insertError(Map<String, Object> row);
    void insertUploadHistory(Map<String, Object> row);
    Map<String, Object> upload(Map<String, Object> query);
    Map<String, Object> lockUpload(Map<String, Object> query);
    List<Map<String, Object>> staging(String uploadId);
    List<Map<String, Object>> errors(String uploadId);
    String errorFile(String uploadId);
    void commitHistory(String uploadId);
    List<Map<String, Object>> histories(Map<String, Object> query);
    void commitUpload(String uploadId);
}
