package kr.ac.knue.commonfoundation.basic54;

import java.util.List;

public record ReportSearchResponse(List<ReportRow> reports, int page, int pageSize, long totalElements) {
}
