package kr.ac.knue.commonfoundation.basic48;

import java.time.LocalDateTime;

public record EvaluationSnapshotRow(
        String snapshotId,
        String evaluationYear,
        String finalizationPoint,
        String organizationCode,
        Long targetUserId,
        String ruleSnapshotRef,
        String materialSnapshotRef,
        String preservedResultRef,
        String snapshotStatus,
        LocalDateTime capturedAt,
        String requestId) {
}
