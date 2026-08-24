package kr.ac.knue.commonfoundation.functionpermissions;

import java.util.List;

public record FunctionPermissionSearchResponse(
        List<FunctionPermissionRow> permissions,
        int page,
        int size,
        long totalElements) {
}
