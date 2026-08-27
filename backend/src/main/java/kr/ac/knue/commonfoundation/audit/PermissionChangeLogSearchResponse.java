package kr.ac.knue.commonfoundation.audit;

import java.util.List;

public record PermissionChangeLogSearchResponse(
        List<PermissionChangeLogRow> logs,
        int page,
        int size,
        long totalElements) {
}
