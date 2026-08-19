package kr.ac.knue.commonfoundation.roles;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class RoleUpdateRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("roleName", "purpose", "assignmentCriteria", "defaultDataScope", "changeReason");

    @NotBlank(message = "역할명을 입력하세요.")
    @Size(max = 100, message = "역할명은 100자 이하여야 합니다.")
    private String roleName;
    @NotBlank(message = "역할 목적을 입력하세요.")
    @Size(max = 500, message = "역할 목적은 500자 이하여야 합니다.")
    private String purpose;
    @NotBlank(message = "부여 기준을 입력하세요.")
    @Size(max = 1000, message = "부여 기준은 1000자 이하여야 합니다.")
    private String assignmentCriteria;
    @NotBlank(message = "데이터 범위 기본값을 입력하세요.")
    @Size(max = 200, message = "데이터 범위 기본값은 200자 이하여야 합니다.")
    private String defaultDataScope;
    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getAssignmentCriteria() {
        return assignmentCriteria;
    }

    public void setAssignmentCriteria(String assignmentCriteria) {
        this.assignmentCriteria = assignmentCriteria;
    }

    public String getDefaultDataScope() {
        return defaultDataScope;
    }

    public void setDefaultDataScope(String defaultDataScope) {
        this.defaultDataScope = defaultDataScope;
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
