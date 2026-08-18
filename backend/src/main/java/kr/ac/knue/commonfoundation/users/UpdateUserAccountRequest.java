package kr.ac.knue.commonfoundation.users;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateUserAccountRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("systemUseYn", "changeReason");
    @NotBlank(message = "시스템 사용여부를 입력하세요.")
    private String systemUseYn;
    @NotBlank(message = "변경 사유를 입력하세요.")
    private String changeReason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

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
