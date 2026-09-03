package kr.ac.knue.commonfoundation.basic45;

import java.math.BigDecimal;

public record FinalEvaluationConfirmationTarget(
        Long targetId,
        String evaluationYear,
        String areaCode,
        String organizationName,
        String targetName,
        String confirmationStatus,
        Long confirmedBy,
        String confirmedByName,
        String confirmedAt,
        Long canceledBy,
        String canceledByName,
        String canceledAt,
        String cancelReason,
        int materialCount,
        BigDecimal totalScore) {
}
