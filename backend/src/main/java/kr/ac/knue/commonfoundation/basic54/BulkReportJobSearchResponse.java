package kr.ac.knue.commonfoundation.basic54;

import java.util.List;

public record BulkReportJobSearchResponse(List<BulkReportJobRow> jobs, int page, int pageSize, long totalElements) {
}
