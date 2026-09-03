package kr.ac.knue.commonfoundation.basic45;

import java.util.List;

public record EvaluationMaterialDeletionPreviewResponse(
        List<EvaluationMaterialDeletionTarget> targets,
        int page,
        int size,
        long totalElements,
        long targetCount) {
    public EvaluationMaterialDeletionPreviewResponse {
        targets = List.copyOf(targets == null ? List.of() : targets);
    }
}
