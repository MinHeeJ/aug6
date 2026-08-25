package kr.ac.knue.commonfoundation.privacy;

import java.util.List;

public record PrivacyAccessPermissionSearchResponse(
        List<PrivacyAccessPermissionRow> permissions,
        int page,
        int size,
        long totalElements) {
}
