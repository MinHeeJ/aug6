package kr.ac.knue.commonfoundation.privacy;

import java.util.List;

public record PrivacyAccessLogSearchResponse(List<PrivacyAccessLogRow> logs, int page, int size, long totalElements) {
}
