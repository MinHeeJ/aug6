package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;

/** D12 read envelope plus source-backed form options. */
public record CourseOperationSearchResponse(
        List<CourseOperationRow> achievements, int page, int pageSize, long totalElements,
        List<CourseOperationManagementItem> managementItems) {
}
