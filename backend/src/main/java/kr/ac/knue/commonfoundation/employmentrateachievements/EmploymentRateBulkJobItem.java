package kr.ac.knue.commonfoundation.employmentrateachievements;

/** One target result of a bulk job; rows are persisted rather than synthesized for the response. */
public record EmploymentRateBulkJobItem(String targetKey, String processingStatus, String reason) {
}
