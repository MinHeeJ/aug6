package kr.ac.knue.commonfoundation.basic32;

import java.util.List;

public record DataChangeHistorySearchResponse(
        List<DataChangeHistoryRow> histories,
        int page,
        int size,
        long totalElements) {
}
