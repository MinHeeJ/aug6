package kr.ac.knue.commonfoundation.basic48;

import java.time.LocalDateTime;

public record EvaluationSnapshotDetail(
        String snapshotId,
        String evaluationYear,
        String finalizationPoint,
        String organizationCode,
        Long targetUserId,
        String ruleSnapshotRef,
        String materialSnapshotRef,
        String preservedResultRef,
        String snapshotStatus,
        String ruleSnapshotJson,
        String materialSnapshotJson,
        String preservedResultJson,
        LocalDateTime capturedAt,
        String requestId,
        String readOnlyNotice) {
}
