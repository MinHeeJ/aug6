package kr.ac.knue.commonfoundation.basic48;

import java.util.List;

public record EvaluationSnapshotSearchResponse(
        List<EvaluationSnapshotRow> results,
        int page,
        int size,
        long totalElements) {
}
