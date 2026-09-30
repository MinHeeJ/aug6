package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** MyBatis adapter for student-guidance source rows and the shared Excel staging/history tables. */
@Mapper
public interface StudentGuidanceAchievementMapper {
    String findOrganizationCodeForUser(@Param("userId") Long userId);
    List<StudentGuidanceAchievementRow> list(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);
    long count(@Param("userId") Long userId);
    void insertAchievement(@Param("row") StudentGuidanceAchievementRow row, @Param("organizationCode") String organizationCode, @Param("createdBy") Long createdBy, @Param("changeReason") String changeReason);
    Long findAchievementIdByManagementNo(@Param("managementNo") String managementNo);
    void insertStudent(@Param("achievementId") Long achievementId, @Param("student") StudentGuidanceStudentRequest student, @Param("createdBy") Long createdBy);
    int existsDuplicate(@Param("teacherUserId") Long teacherUserId, @Param("studentNo") String studentNo, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    int countTemplate(@Param("templateId") String templateId);
    void insertUploadFile(@Param("uploadId") String uploadId, @Param("templateId") String templateId, @Param("fileToken") String fileToken, @Param("originalFileName") String originalFileName, @Param("userId") Long userId, @Param("validationStatus") String validationStatus);
    void insertStagingRow(@Param("stagingRowId") String stagingRowId, @Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("payload") String payload, @Param("validationStatus") String validationStatus);
    void insertUploadError(@Param("errorId") String errorId, @Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("columnName") String columnName, @Param("inputValue") String inputValue, @Param("errorCode") String errorCode, @Param("errorReason") String errorReason);
    void upsertUploadHistory(@Param("uploadId") String uploadId, @Param("totalCount") int totalCount, @Param("successCount") int successCount, @Param("errorCount") int errorCount, @Param("savedCount") int savedCount, @Param("userId") Long userId);
    int existsUpload(@Param("uploadId") String uploadId);
    int countErrors(@Param("uploadId") String uploadId);
    List<StudentGuidanceStagingRow> listNormalStagingRows(@Param("uploadId") String uploadId);
    void markCommitted(@Param("uploadId") String uploadId);
    void deleteNormalStagingRows(@Param("uploadId") String uploadId);
    List<kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow> listHistories(@Param("limit") int limit, @Param("offset") int offset);
}
