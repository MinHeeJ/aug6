package kr.ac.knue.commonfoundation.codes;

import java.util.List;

public record DetailCodeUsageSearchResponse(
        List<DetailCodeUsageRow> settings,
        List<DetailCodeUsageRow> selectableOptions,
        int page,
        int size,
        long totalElements) {
}
