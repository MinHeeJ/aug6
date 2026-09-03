package kr.ac.knue.commonfoundation.basic46;

import java.util.List;

public record EvaluationMaterialDeletionPreviewResponse(
        List<EvaluationMaterialDeletionTarget> targets,
        int page,
        int size,
        long totalElements,
        long deletableCount,
        String previewToken) {
}
