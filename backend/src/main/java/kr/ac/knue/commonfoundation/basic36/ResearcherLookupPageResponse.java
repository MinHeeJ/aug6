package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record ResearcherLookupPageResponse<T>(List<T> rows, int page, int pageSize, long totalElements) {
}
