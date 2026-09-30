package kr.ac.knue.commonfoundation.achievement;
import java.util.List;
/** Validation result for a STUDENT_GUIDANCE upload; error rows are never eligible for commit. */
public record StudentGuidanceUploadResult(String uploadId, String originalFileName, String validationStatus, int totalCount, int successCount, int errorCount, int savedCount, List<StudentGuidanceUploadError> errors) {}
