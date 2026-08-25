package kr.ac.knue.commonfoundation.notices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NoticeTargetInput {
    @NotBlank(message = "대상 유형을 선택하세요.")
    @Pattern(regexp = "ROLE|ORGANIZATION", message = "대상 유형은 ROLE 또는 ORGANIZATION이어야 합니다.")
    private String targetType;

    @NotBlank(message = "대상 식별자를 입력하세요.")
    @Size(max = 100, message = "대상 식별자는 100자 이하여야 합니다.")
    private String targetId;

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
