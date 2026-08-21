package kr.ac.knue.commonfoundation.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DutyAssignmentRow(
        Long dutyAssignmentId,
        String dutyOrganization,
        Long userId,
        String userName,
        String dutyArea,
        LocalDate validStartDate,
        LocalDate validEndDate,
        String dataScopeType,
        String processingPermission,
        String status,
        LocalDateTime confirmedAt,
        String changeReason,
        LocalDateTime updatedAt
) {
    @JsonProperty("assignmentId")
    public Long assignmentId() {
        return dutyAssignmentId;
    }
}
