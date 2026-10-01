package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis boundary for student-guidance headers, students, and Excel staging-to-business commits. */
@Mapper
public interface StudentGuidanceAchievementMapper {
    List<StudentGuidanceAchievementRow> list(@Param("criteria") StudentGuidanceAchievementSearchCriteria criteria);
    long count(@Param("criteria") StudentGuidanceAchievementSearchCriteria criteria);
    StudentGuidanceAchievementRow findById(@Param("achievementId") Long achievementId);
    String findActiveOrganizationCode(@Param("userId") Long userId);
    Long findUserIdByEmployeeNo(@Param("employeeNo") String employeeNo);
    int insertAchievement(@Param("managementNo") String managementNo, @Param("teacherUserId") Long teacherUserId, @Param("evaluationYear") String evaluationYear, @Param("organizationCode") String organizationCode, @Param("managementItemCode") String managementItemCode, @Param("guidanceStartDate") LocalDate guidanceStartDate, @Param("guidanceEndDate") LocalDate guidanceEndDate, @Param("attachmentRef") String attachmentRef, @Param("createdBy") Long createdBy, @Param("changeReason") String changeReason);
    int updateAchievement(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode, @Param("guidanceStartDate") LocalDate guidanceStartDate, @Param("guidanceEndDate") LocalDate guidanceEndDate, @Param("attachmentRef") String attachmentRef, @Param("updatedBy") Long updatedBy, @Param("changeReason") String changeReason);
    int deleteStudents(@Param("achievementId") Long achievementId);
    int insertStudent(@Param("achievementId") Long achievementId, @Param("student") StudentGuidanceStudentRequest student, @Param("userId") Long userId);
    int insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);
    int insertUploadFile(@Param("uploadId") String uploadId, @Param("originalFileName") String originalFileName, @Param("userId") Long userId, @Param("status") String status);
    int insertStagingRow(@Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("payload") String payload, @Param("status") String status);
    int insertUploadError(@Param("errorId") String errorId, @Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("columnName") String columnName, @Param("inputValue") String inputValue, @Param("errorCode") String errorCode, @Param("errorReason") String errorReason, @Param("correctionGuide") String correctionGuide);
    int upsertUploadHistory(@Param("uploadId") String uploadId, @Param("total") int total, @Param("success") int success, @Param("error") int error, @Param("saved") int saved, @Param("userId") Long userId);
    int countUploadErrors(@Param("uploadId") String uploadId);
    int existsValidatedUpload(@Param("uploadId") String uploadId, @Param("userId") Long userId);
    List<String> normalStagingPayloads(@Param("uploadId") String uploadId);
    int markUploadCommitted(@Param("uploadId") String uploadId);
    int deleteStagingRows(@Param("uploadId") String uploadId);
    List<kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow> listUploadErrors(@Param("uploadId") String uploadId);
    List<kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow> listUploadHistories(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);
}
