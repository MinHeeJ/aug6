package kr.ac.knue.commonfoundation.batch;

import java.util.List;

public record BatchRetryTargetSearchResponse(
        List<BatchRetryTargetRow> targets,
        int page,
        int size,
        long totalElements) {
}
