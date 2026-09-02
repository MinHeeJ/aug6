package kr.ac.knue.commonfoundation.basic43;

import java.util.List;

public record GrantPaymentApprovalSearchResponse(
        List<GrantPaymentApprovalRow> approvals,
        int page,
        int size,
        long totalElements) {
}
