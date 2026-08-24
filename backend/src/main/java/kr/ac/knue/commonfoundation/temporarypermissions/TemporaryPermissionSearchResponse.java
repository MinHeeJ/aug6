package kr.ac.knue.commonfoundation.temporarypermissions;

import java.util.List;

public record TemporaryPermissionSearchResponse(
        List<TemporaryPermissionRow> permissions,
        int page,
        int size,
        long totalElements) {
}
