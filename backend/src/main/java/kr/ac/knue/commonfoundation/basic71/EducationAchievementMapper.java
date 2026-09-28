package kr.ac.knue.commonfoundation.basic71;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence port for education achievements, their children, and immutable audit events. */
@Mapper
public interface EducationAchievementMapper {
    List<EducationAchievementRow> list(@Param("criteria") EducationAchievementSearchCriteria criteria, @Param("viewer") Long viewer);
    long count(@Param("criteria") EducationAchievementSearchCriteria criteria, @Param("viewer") Long viewer);
    EducationAchievementRow find(@Param("achievementId") Long achievementId);
    EducationAchievementRow insert(@Param("request") SaveEducationAchievementRequest request, @Param("evaluationYear") String evaluationYear, @Param("detailsJson") String detailsJson, @Param("teacherUserId") Long teacherUserId);
    void insertAttachment(@Param("achievementId") Long achievementId, @Param("fileToken") String fileToken, @Param("sortOrder") int sortOrder);
    void insertDegreeStudent(@Param("achievementId") Long achievementId, @Param("student") GraduateDegreeCompletionStudentRequest student, @Param("sortOrder") int sortOrder);
    List<String> listAttachmentFileTokens(@Param("achievementId") Long achievementId);
    List<GraduateDegreeCompletionStudentRow> listDegreeStudents(@Param("achievementId") Long achievementId);
    int updateStatus(@Param("achievementId") Long achievementId, @Param("expectedStatus") String expectedStatus, @Param("nextStatus") String nextStatus, @Param("updatedBy") Long updatedBy);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("previousStatus") String previousStatus, @Param("nextStatus") String nextStatus, @Param("actionType") String actionType, @Param("processedBy") Long processedBy, @Param("reasonCode") String reasonCode, @Param("opinion") String opinion, @Param("requestId") String requestId);
    void insertChangeHistory(@Param("achievementId") Long achievementId, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("requestId") String requestId);
    int educationAchievementExists(@Param("teacherUserId") Long teacherUserId, @Param("managementItemCode") String managementItemCode, @Param("occurredOn") LocalDate occurredOn);
    void insertExcelUploadFile(@Param("uploadId") String uploadId, @Param("fileToken") String fileToken, @Param("originalFileName") String originalFileName, @Param("uploaderUserId") Long uploaderUserId);
    void insertStudentGuidanceStaging(@Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("payload") String payload, @Param("status") String status);
    void insertStudentGuidanceError(@Param("uploadId") String uploadId, @Param("error") StudentGuidanceUploadError error);
}
