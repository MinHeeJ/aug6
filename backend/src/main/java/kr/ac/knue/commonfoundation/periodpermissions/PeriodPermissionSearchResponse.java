package kr.ac.knue.commonfoundation.periodpermissions;

import java.util.List;

public record PeriodPermissionSearchResponse(List<PeriodPermissionRow> links, int page, int size, long totalElements) {
}
