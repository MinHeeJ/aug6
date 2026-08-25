package kr.ac.knue.commonfoundation.notices;

import java.util.List;

public record NoticeSearchResponse(List<NoticeSummaryRow> notices, int page, int pageSize, long totalElements) {
}
