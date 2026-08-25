package kr.ac.knue.commonfoundation.manuals;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ManualRow(
        Long manualId,
        String manualType,
        String version,
        String targetUser,
        LocalDate effectiveDate,
        String originalFileName,
        Boolean latest,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {
}
