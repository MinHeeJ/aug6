package kr.ac.knue.commonfoundation.basic54;

import java.util.List;

public record BulkReportTargetSearchResponse(List<BulkReportJobTargetRow> targets, int page, int pageSize, long totalElements) {
}
