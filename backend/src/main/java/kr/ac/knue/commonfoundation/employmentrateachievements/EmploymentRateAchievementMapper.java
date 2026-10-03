package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for source records, upload diagnostics, and bulk-job result rows. */
@Mapper
public interface EmploymentRateAchievementMapper {
    List<EmploymentRateAchievementRow> list(@Param("page") int page, @Param("pageSize") int pageSize,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    long count(@Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    EmploymentRateAchievementRow find(@Param("achievementId") Long achievementId);
    EmploymentRateAchievementRow findByManagementNo(@Param("managementNo") String managementNo);
    void insert(@Param("managementNo") String managementNo, @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear, @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate, @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds, @Param("userId") Long userId);
    void update(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate, @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds, @Param("userId") Long userId);
    void insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy, @Param("requestId") String requestId);
    Long findUserIdByEmployeeNo(@Param("employeeNo") String employeeNo);
    int countDuplicate(@Param("targetUserId") Long targetUserId, @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate, @Param("achievementName") String achievementName);
    void insertUpload(@Param("uploadId") String uploadId, @Param("fileName") String fileName,
            @Param("userId") Long userId, @Param("status") String status, @Param("total") int total,
            @Param("success") int success, @Param("errors") int errors);
    void insertUploadError(@Param("uploadId") String uploadId, @Param("error") EmploymentRateExcelError error);
    EmploymentRateBulkJobResult findBulkJob(@Param("jobId") String jobId, @Param("operatorUserId") Long operatorUserId);
}
