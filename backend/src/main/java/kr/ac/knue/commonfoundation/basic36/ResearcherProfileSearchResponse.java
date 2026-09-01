package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record ResearcherProfileSearchResponse(List<ResearcherProfileSummary> profiles, int page, int pageSize, long totalElements) {
}
