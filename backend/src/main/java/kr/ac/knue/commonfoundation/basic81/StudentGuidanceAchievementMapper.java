package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for student-guidance headers, details, and R07 upload commits. */
@Mapper
public interface StudentGuidanceAchievementMapper {
    List<StudentGuidanceAchievementRow> list(@Param("criteria") StudentGuidanceAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    long count(@Param("criteria") StudentGuidanceAchievementSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    StudentGuidanceAchievementRow find(@Param("achievementId") Long achievementId);
    StudentGuidanceAchievementRow findByManagementNo(@Param("managementNo") String managementNo);
    void insertHeader(@Param("managementNo") String managementNo, @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear, @Param("managementItemCode") String managementItemCode,
            @Param("guidanceStartDate") LocalDate guidanceStartDate, @Param("guidanceEndDate") LocalDate guidanceEndDate,
            @Param("studentCount") int studentCount, @Param("attachmentRef") String attachmentRef, @Param("createdBy") Long createdBy);
    void updateHeader(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode,
            @Param("guidanceStartDate") LocalDate guidanceStartDate, @Param("guidanceEndDate") LocalDate guidanceEndDate,
            @Param("studentCount") int studentCount, @Param("attachmentRef") String attachmentRef, @Param("updatedBy") Long updatedBy);
    void deleteStudents(@Param("achievementId") Long achievementId);
    void insertStudent(@Param("achievementId") Long achievementId, @Param("studentDetail") String studentDetail,
            @Param("userId") Long userId);
    void insertStatusHistory(@Param("history") EducationAchievementStatusHistory history);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey,
            @Param("changeType") String changeType, @Param("fieldName") String fieldName,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason,
            @Param("requestId") String requestId);
    void insertExcelUpload(@Param("uploadId") String uploadId, @Param("templateId") String templateId,
            @Param("originalFileName") String originalFileName, @Param("userId") Long userId,
            @Param("validationStatus") String validationStatus);
    void insertExcelStaging(@Param("stagingRowId") String stagingRowId, @Param("uploadId") String uploadId,
            @Param("rowNumber") int rowNumber, @Param("payload") String payload,
            @Param("validationStatus") String validationStatus);
    void insertExcelError(@Param("errorId") String errorId, @Param("uploadId") String uploadId,
            @Param("rowNumber") int rowNumber, @Param("columnName") String columnName,
            @Param("inputValue") String inputValue, @Param("errorCode") String errorCode,
            @Param("errorReason") String errorReason, @Param("correctionGuide") String correctionGuide);
    void upsertExcelHistory(@Param("uploadId") String uploadId, @Param("totalCount") int totalCount,
            @Param("successCount") int successCount, @Param("errorCount") int errorCount,
            @Param("savedCount") int savedCount, @Param("userId") Long userId);
    int countExcelErrors(@Param("uploadId") String uploadId);
    int existsExcelUpload(@Param("uploadId") String uploadId);
    List<StudentGuidanceExcelStagingRow> listNormalExcelStaging(@Param("uploadId") String uploadId);
    void markExcelCommitted(@Param("uploadId") String uploadId);
    void deleteNormalExcelStaging(@Param("uploadId") String uploadId);
    List<StudentGuidanceExcelErrorRow> listExcelErrors(@Param("uploadId") String uploadId);
    List<StudentGuidanceExcelHistoryRow> listExcelHistories(@Param("userId") Long userId);
}
