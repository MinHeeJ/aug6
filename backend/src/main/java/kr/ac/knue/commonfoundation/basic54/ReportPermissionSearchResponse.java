package kr.ac.knue.commonfoundation.basic54;

import java.util.List;

public record ReportPermissionSearchResponse(List<ReportPermissionRow> permissions, int page, int pageSize, long totalElements) {
}
