package kr.ac.knue.commonfoundation.basic54;

public record BulkReportJobTargetRow(Long jobTargetId, Long jobId, Long targetPersonId, String targetPersonName,
                                     String targetOrganizationCode, String resultCode, String errorDetail) {
}
