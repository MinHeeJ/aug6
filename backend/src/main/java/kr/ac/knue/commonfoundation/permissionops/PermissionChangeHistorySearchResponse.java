package kr.ac.knue.commonfoundation.permissionops;

import java.util.List;

public record PermissionChangeHistorySearchResponse(
        List<PermissionChangeHistoryRow> history,
        int page,
        int size,
        long total
) {
}
