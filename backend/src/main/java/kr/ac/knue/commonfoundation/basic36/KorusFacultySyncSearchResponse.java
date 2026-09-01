package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record KorusFacultySyncSearchResponse(List<KorusFacultySyncResultRow> results, int page, int pageSize,
                                             long totalElements) {
}
