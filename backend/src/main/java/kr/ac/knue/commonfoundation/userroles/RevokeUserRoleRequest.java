package kr.ac.knue.commonfoundation.userroles;

import jakarta.validation.constraints.NotBlank;

public class RevokeUserRoleRequest {
    @NotBlank(message = "변경 사유를 입력하세요.")
    private String changeReason;

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
