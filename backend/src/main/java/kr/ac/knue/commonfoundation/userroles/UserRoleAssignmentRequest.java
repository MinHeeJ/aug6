package kr.ac.knue.commonfoundation.userroles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class UserRoleAssignmentRequest {
    @NotBlank(message = "사용자를 선택하세요.")
    private String userId;
    @NotBlank(message = "역할을 선택하세요.")
    private String roleCode;
    @NotBlank(message = "역할 구분을 선택하세요.")
    private String assignmentType;
    @NotNull(message = "유효 시작일을 입력하세요.")
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    private String changeReason;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public LocalDate getValidStartDate() {
        return validStartDate;
    }

    public void setValidStartDate(LocalDate validStartDate) {
        this.validStartDate = validStartDate;
    }

    public LocalDate getValidEndDate() {
        return validEndDate;
    }

    public void setValidEndDate(LocalDate validEndDate) {
        this.validEndDate = validEndDate;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
