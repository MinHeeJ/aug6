package kr.ac.knue.commonfoundation.securitysessions;

import java.util.List;

public record SessionTerminationHistorySearchResponse(List<SessionTerminationHistoryRow> histories,
        int page, int size, long totalElements) {
}
