package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Per-target outcome shown by the employment-rate bulk processing result screen. */
public record EmploymentRateBulkJobItem(
        Long targetUserId,
        Long achievementId,
        boolean processed,
        String unprocessedReason) {
}
