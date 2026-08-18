package kr.ac.knue.commonfoundation.codes;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class CodeGroupRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("groupId", "groupName", "description", "managingDepartment", "systemUseYn", "changeReason");

    @NotBlank(message = "그룹ID를 입력하세요.")
    @Size(max = 100, message = "그룹ID는 100자 이하여야 합니다.")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "그룹ID는 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다.")
    private String groupId;
    @NotBlank(message = "명칭을 입력하세요.")
    @Size(max = 200, message = "명칭은 200자 이하여야 합니다.")
    private String groupName;
    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    private String description;
    @NotBlank(message = "관리부서를 입력하세요.")
    @Size(max = 200, message = "관리부서는 200자 이하여야 합니다.")
    private String managingDepartment;
    @Pattern(regexp = "Y|N", message = "사용여부는 Y 또는 N이어야 합니다.")
    private String systemUseYn = "Y";
    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagingDepartment() {
        return managingDepartment;
    }

    public void setManagingDepartment(String managingDepartment) {
        this.managingDepartment = managingDepartment;
    }

    public String getSystemUseYn() {
        return systemUseYn;
    }

    public void setSystemUseYn(String systemUseYn) {
        this.systemUseYn = systemUseYn;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public Set<String> getUnexpectedFields() {
        return unexpectedFields;
    }

    @JsonAnySetter
    public void captureUnexpectedField(String field, Object ignored) {
        if (!ALLOWED_FIELDS.contains(field)) {
            unexpectedFields.add(field);
        }
    }
}
