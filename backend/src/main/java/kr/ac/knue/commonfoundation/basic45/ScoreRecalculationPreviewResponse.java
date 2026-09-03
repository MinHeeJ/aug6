package kr.ac.knue.commonfoundation.basic45;

import java.util.List;

public record ScoreRecalculationPreviewResponse(
        List<ScoreRecalculationTarget> targets,
        int page,
        int size,
        long totalElements,
        long targetCount) {
}
