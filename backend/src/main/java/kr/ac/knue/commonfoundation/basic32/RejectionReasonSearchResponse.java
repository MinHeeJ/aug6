package kr.ac.knue.commonfoundation.basic32;

import java.util.List;

public record RejectionReasonSearchResponse(
        List<RejectionReasonRow> reasons,
        int page,
        int size,
        long totalElements) {
}
