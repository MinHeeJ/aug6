package kr.ac.knue.commonfoundation.audit;

import java.util.List;

public record BusinessProcessLogSearchResponse(
        List<BusinessProcessLogRow> logs,
        int page,
        int size,
        long totalElements) {
}
