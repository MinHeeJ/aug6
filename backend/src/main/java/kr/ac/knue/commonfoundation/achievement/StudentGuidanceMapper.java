package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis boundary for student-guidance source rows and the existing Excel staging/history tables. */
@Mapper
public interface StudentGuidanceMapper {
    StudentGuidanceDtos.Header findById(@Param("achievementId") Long achievementId);
    List<StudentGuidanceDtos.Student> listStudents(@Param("achievementId") Long achievementId);
    int insertAchievement(@Param("request") StudentGuidanceDtos.SaveRequest request, @Param("ownerUserId") Long ownerUserId, @Param("createdBy") Long createdBy);
    Long findCreatedId(@Param("request") StudentGuidanceDtos.SaveRequest request, @Param("ownerUserId") Long ownerUserId);
    int updateAchievement(@Param("request") StudentGuidanceDtos.SaveRequest request, @Param("updatedBy") Long updatedBy);
    void deleteStudents(@Param("achievementId") Long achievementId);
    void insertStudent(@Param("achievementId") Long achievementId, @Param("student") StudentGuidanceDtos.Student student, @Param("userId") Long userId);
    void insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("userId") Long userId, @Param("reason") String reason);
    int countTemplate();
    void insertUploadFile(@Param("uploadId") String uploadId, @Param("originalFileName") String originalFileName, @Param("userId") Long userId, @Param("status") String status);
    void updateUploadValidationStatus(@Param("uploadId") String uploadId, @Param("status") String status);
    void insertStaging(@Param("stagingId") String stagingId, @Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("payload") String payload, @Param("status") String status);
    void insertError(@Param("errorId") String errorId, @Param("uploadId") String uploadId, @Param("error") StudentGuidanceDtos.ExcelRowError error);
    void upsertHistory(@Param("uploadId") String uploadId, @Param("total") int total, @Param("success") int success, @Param("errors") int errors, @Param("saved") int saved, @Param("userId") Long userId);
    int countErrors(@Param("uploadId") String uploadId);
    int existsUploadForUser(@Param("uploadId") String uploadId, @Param("userId") Long userId);
    int existsActiveManagementItem(@Param("evaluationYear") String evaluationYear, @Param("managementItemCode") String managementItemCode);
    List<String> listNormalPayloads(@Param("uploadId") String uploadId);
    int markCommitted(@Param("uploadId") String uploadId);
    void deleteStaging(@Param("uploadId") String uploadId);
    List<StudentGuidanceDtos.ExcelRowError> listErrors(@Param("uploadId") String uploadId);
    List<StudentGuidanceDtos.UploadHistory> listHistories(@Param("uploaderUserId") Long uploaderUserId);
    Long findUserIdByEmployeeNo(@Param("employeeNo") String employeeNo);
    String findOrganizationForUser(@Param("userId") Long userId);
    int existsDuplicate(@Param("ownerUserId") Long ownerUserId, @Param("guidanceStartDate") LocalDate start, @Param("guidanceEndDate") LocalDate end, @Param("studentName") String studentName);
    void insertImportedAchievement(@Param("evaluationYear") String evaluationYear, @Param("ownerUserId") Long ownerUserId, @Param("organizationCode") String organizationCode, @Param("managementItemCode") String managementItemCode, @Param("guidanceStartDate") LocalDate start, @Param("guidanceEndDate") LocalDate end, @Param("studentName") String studentName, @Param("userId") Long userId);
}
