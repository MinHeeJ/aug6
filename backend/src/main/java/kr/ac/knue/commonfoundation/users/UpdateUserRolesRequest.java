package kr.ac.knue.commonfoundation.users;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UpdateUserRolesRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("roleCodes", "validStartDate", "validEndDate", "changeReason");
    @NotEmpty(message = "업무 역할을 하나 이상 선택하세요.")
    private List<String> roleCodes;
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    @NotBlank(message = "변경 사유를 입력하세요.")
    private String changeReason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
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
