package kr.ac.knue.commonfoundation.operations;

import jakarta.validation.constraints.NotBlank;

public class DataScopeRulesSaveRequest {
    @NotBlank(message = "역할코드를 선택하세요.")
    private String roleCode;
    @NotBlank(message = "데이터 범위 유형을 선택하세요.")
    private String dataScopeType;
    private String organizationCode;
    private String dutyArea;
    private String changeReason;

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getDataScopeType() { return dataScopeType; }
    public void setDataScopeType(String dataScopeType) { this.dataScopeType = dataScopeType; }
    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    public String getDutyArea() { return dutyArea; }
    public void setDutyArea(String dutyArea) { this.dutyArea = dutyArea; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
