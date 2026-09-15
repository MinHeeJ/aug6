package kr.ac.knue.commonfoundation.basic65;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentGuidanceAchievementMapper {
    List<StudentGuidanceAchievementRow> list(@Param("criteria") StudentGuidanceAchievementSearchCriteria criteria, @Param("facultyUserId") Long facultyUserId);
    long count(@Param("criteria") StudentGuidanceAchievementSearchCriteria criteria, @Param("facultyUserId") Long facultyUserId);
    StudentGuidanceAchievementRow findByIdAndFaculty(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId);
    StudentGuidanceAchievementRow findDuplicate(@Param("facultyUserId") Long facultyUserId, @Param("studentNo") String studentNo, @Param("guidanceType") String guidanceType, @Param("guidanceDate") String guidanceDate);
    void insert(@Param("request") SaveStudentGuidanceAchievementRequest request, @Param("facultyUserId") Long facultyUserId, @Param("dynamicFieldsJson") String dynamicFieldsJson, @Param("attachmentRefsJson") String attachmentRefsJson);
    void update(@Param("request") SaveStudentGuidanceAchievementRequest request, @Param("facultyUserId") Long facultyUserId, @Param("dynamicFieldsJson") String dynamicFieldsJson, @Param("attachmentRefsJson") String attachmentRefsJson);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId, @Param("reason") String reason, @Param("requestId") String requestId);
    void insertUpload(@Param("uploadId") String uploadId, @Param("fileToken") String fileToken, @Param("originalFileName") String originalFileName, @Param("facultyUserId") Long facultyUserId, @Param("validationStatus") String validationStatus);
    void upsertUploadHistory(@Param("uploadId") String uploadId, @Param("totalCount") int totalCount, @Param("successCount") int successCount, @Param("errorCount") int errorCount, @Param("savedCount") int savedCount, @Param("facultyUserId") Long facultyUserId);
    void insertUploadError(@Param("uploadId") String uploadId, @Param("rowNumber") int rowNumber, @Param("columnName") String columnName, @Param("errorCode") String errorCode, @Param("errorReason") String errorReason);
    List<StudentGuidanceExcelRowError> listUploadErrors(@Param("uploadId") String uploadId);
}
