package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class PositionAssignmentRequest {
    @NotBlank(message = "보직코드를 입력하세요.")
    private String positionCode;
    @NotBlank(message = "대상 사용자를 선택하세요.")
    private String userId;
    @NotBlank(message = "소속조직을 선택하세요.")
    private String organizationCode;
    @NotNull(message = "유효 시작일을 입력하세요.")
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    @NotBlank(message = "변경 사유를 입력하세요.")
    private String changeReason;

    public String getPositionCode() { return positionCode; }
    public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public void setEffectiveStartDate(LocalDate effectiveStartDate) { this.effectiveStartDate = effectiveStartDate; }
    public LocalDate getEffectiveEndDate() { return effectiveEndDate; }
    public void setEffectiveEndDate(LocalDate effectiveEndDate) { this.effectiveEndDate = effectiveEndDate; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
