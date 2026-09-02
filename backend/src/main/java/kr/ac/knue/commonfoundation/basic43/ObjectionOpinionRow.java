package kr.ac.knue.commonfoundation.basic43;

import java.time.LocalDateTime;

public record ObjectionOpinionRow(
        Long objectionOpinionId,
        Long objectionId,
        String evaluationYear,
        Long applicantUserId,
        String applicantOpinionSnapshot,
        String objectionContentSnapshot,
        String reviewerOpinion,
        String decisionResult,
        String reasonCode,
        Long processedBy,
        LocalDateTime processedAt,
        String changeReason) {
}
