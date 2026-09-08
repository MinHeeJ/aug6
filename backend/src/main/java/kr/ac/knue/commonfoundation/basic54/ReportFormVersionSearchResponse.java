package kr.ac.knue.commonfoundation.basic54;

import java.util.List;

public record ReportFormVersionSearchResponse(List<ReportFormVersionRow> formVersions, int page, int pageSize, long totalElements) {
}
