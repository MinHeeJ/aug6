package kr.ac.knue.commonfoundation.basic46;

import java.util.List;

public record EvaluationMaterialGenerationSearchResponse(
        List<EvaluationMaterialGenerationTarget> targets,
        int page,
        int size,
        long totalElements) {
}
