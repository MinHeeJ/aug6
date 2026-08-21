package kr.ac.knue.commonfoundation.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PositionAssignmentRow(
        Long positionAssignmentId,
        String positionCode,
        Long userId,
        String userName,
        String organizationCode,
        String organizationName,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String status,
        LocalDateTime confirmedAt,
        String changeReason,
        LocalDateTime updatedAt
) {
    @JsonProperty("assignmentId")
    public Long assignmentId() {
        return positionAssignmentId;
    }
}
