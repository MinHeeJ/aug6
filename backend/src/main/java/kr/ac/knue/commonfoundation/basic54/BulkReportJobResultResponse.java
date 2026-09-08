package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDateTime;
import java.util.List;

public record BulkReportJobResultResponse(Long jobId, String reportId, String reportName, Long requesterId,
                                          String targetHash, String status, int progressRate, int totalCount,
                                          int successCount, int failCount, String resultFileRef, String requestId,
                                          LocalDateTime requestedAt, LocalDateTime completedAt,
                                          List<BulkReportJobTargetRow> failures) {
    public static BulkReportJobResultResponse from(BulkReportJobRow job, List<BulkReportJobTargetRow> failures) {
        return new BulkReportJobResultResponse(job.jobId(), job.reportId(), job.reportName(), job.requesterId(),
                job.targetHash(), job.status(), job.progressRate(), job.totalCount(), job.successCount(),
                job.failCount(), job.resultFileRef(), job.requestId(), job.requestedAt(), job.completedAt(), failures);
    }
}
