package kr.ac.knue.commonfoundation.basic32;

import java.util.List;

public record BusinessStatusCodeSearchResponse(
        List<BusinessStatusCodeRow> statusCodes,
        int page,
        int size,
        long totalElements) {
}
