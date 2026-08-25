package kr.ac.knue.commonfoundation.privacy;

import java.util.List;

public record PrivacyFieldPolicySearchResponse(
        List<PrivacyFieldPolicyRow> policies,
        int page,
        int size,
        long totalElements) {
}
