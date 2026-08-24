package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;

public record AttachmentIntegrityCheckResponse(
        Long checkId,
        String status,
        Long startedBy,
        String startedAt,
        String completedAt,
        int findingCount,
        List<String> anomalyTypes) {
}
