package kr.ac.knue.commonfoundation.basic54;

import java.util.List;

public record ReportPrintHistorySearchResponse(List<ReportPrintHistoryRow> histories, int page, int pageSize, long totalElements) {
}
