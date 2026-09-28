package kr.ac.knue.commonfoundation.educationachievements;

/** Result of the validate-only upload stage; a failed row prevents all achievement writes. */
public record StudentGuidanceUploadResult(Long uploadHistoryId, int totalCount, int successCount, int failureCount, String errorFileRef) {
}
