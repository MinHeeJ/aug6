package kr.ac.knue.commonfoundation.basic50;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AuthorityRow(
        Long authorityId,
        String evaluationYear,
        String organizationCode,
        String evaluationUnitCode,
        Long managerUserId,
        String managerName,
        String inputAllowedYn,
        String outputAllowedYn,
        String modifyAllowedYn,
        Long teacherUserId,
        String teacherName,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String activeYn,
        String changeReason,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
