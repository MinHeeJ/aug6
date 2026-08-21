package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class DutyAssignmentRequest {
    @NotBlank(message = "업무조직을 입력하세요.")
    private String dutyOrganization;
    @NotBlank(message = "담당자를 선택하세요.")
    private String userId;
    @NotBlank(message = "담당 업무영역을 입력하세요.")
    private String dutyArea;
    @NotNull(message = "지정 시작일을 입력하세요.")
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    @NotBlank(message = "데이터 범위를 선택하세요.")
    private String dataScopeType;
    @NotBlank(message = "처리 권한을 입력하세요.")
    private String processingPermission;
    @NotBlank(message = "변경 사유를 입력하세요.")
    private String changeReason;

    public String getDutyOrganization() { return dutyOrganization; }
    public void setDutyOrganization(String dutyOrganization) { this.dutyOrganization = dutyOrganization; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDutyArea() { return dutyArea; }
    public void setDutyArea(String dutyArea) { this.dutyArea = dutyArea; }
    public LocalDate getValidStartDate() { return validStartDate; }
    public void setValidStartDate(LocalDate validStartDate) { this.validStartDate = validStartDate; }
    public LocalDate getValidEndDate() { return validEndDate; }
    public void setValidEndDate(LocalDate validEndDate) { this.validEndDate = validEndDate; }
    public String getDataScopeType() { return dataScopeType; }
    public void setDataScopeType(String dataScopeType) { this.dataScopeType = dataScopeType; }
    public String getProcessingPermission() { return processingPermission; }
    public void setProcessingPermission(String processingPermission) { this.processingPermission = processingPermission; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
