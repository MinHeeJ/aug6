package kr.ac.knue.commonfoundation.audit;

import java.util.List;

public record SensitiveInformationAccessLogSearchResponse(
        List<SensitiveInformationAccessLogRow> logs,
        int page,
        int size,
        long totalElements) {
}
