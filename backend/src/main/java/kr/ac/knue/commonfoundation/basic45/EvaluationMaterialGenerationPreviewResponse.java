package kr.ac.knue.commonfoundation.basic45;

import java.util.List;

public record EvaluationMaterialGenerationPreviewResponse(
        List<EvaluationMaterialGenerationTarget> targets,
        int page,
        int size,
        long totalElements,
        long targetCount) {
    public EvaluationMaterialGenerationPreviewResponse {
        targets = List.copyOf(targets);
    }
}
