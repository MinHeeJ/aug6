package kr.ac.knue.commonfoundation.manuals;

import java.util.List;

public record ManualSearchResponse(List<ManualRow> manuals, int page, int size, long totalElements) {
}
