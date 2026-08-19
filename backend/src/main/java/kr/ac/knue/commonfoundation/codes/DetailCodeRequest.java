package kr.ac.knue.commonfoundation.codes;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DetailCodeRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "codeValue", "codeName", "parentCodeValue", "sortOrder", "additionalAttributes",
            "systemUseYn", "validStartDate", "validEndDate", "changeReason");

    @NotBlank(message = "코드값을 입력하세요.")
    @Size(max = 100, message = "코드값은 100자 이하여야 합니다.")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "코드값은 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다.")
    private String codeValue;
    @NotBlank(message = "코드명을 입력하세요.")
    @Size(max = 200, message = "코드명은 200자 이하여야 합니다.")
    private String codeName;
    @Size(max = 100, message = "상위코드값은 100자 이하여야 합니다.")
    private String parentCodeValue;
    @NotNull(message = "정렬순서를 입력하세요.")
    @Min(value = 0, message = "정렬순서는 0 이상이어야 합니다.")
    private Integer sortOrder;
    private Map<String, Object> additionalAttributes;
    @Pattern(regexp = "Y|N", message = "사용여부는 Y 또는 N이어야 합니다.")
    private String systemUseYn = "Y";
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;
    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getCodeValue() { return codeValue; }
    public void setCodeValue(String codeValue) { this.codeValue = codeValue; }
    public String getCodeName() { return codeName; }
    public void setCodeName(String codeName) { this.codeName = codeName; }
    public String getParentCodeValue() { return parentCodeValue; }
    public void setParentCodeValue(String parentCodeValue) { this.parentCodeValue = parentCodeValue; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Map<String, Object> getAdditionalAttributes() { return additionalAttributes; }
    public void setAdditionalAttributes(Map<String, Object> additionalAttributes) { this.additionalAttributes = additionalAttributes; }
    public String getSystemUseYn() { return systemUseYn; }
    public void setSystemUseYn(String systemUseYn) { this.systemUseYn = systemUseYn; }
    public LocalDate getValidStartDate() { return validStartDate; }
    public void setValidStartDate(LocalDate validStartDate) { this.validStartDate = validStartDate; }
    public LocalDate getValidEndDate() { return validEndDate; }
    public void setValidEndDate(LocalDate validEndDate) { this.validEndDate = validEndDate; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public Set<String> getUnexpectedFields() { return unexpectedFields; }

    @JsonIgnore
    public boolean hasAdditionalAttributes() {
        return additionalAttributes != null && !additionalAttributes.isEmpty();
    }

    public void putAdditionalAttribute(String key, Object value) {
        if (additionalAttributes == null) {
            additionalAttributes = new LinkedHashMap<>();
        }
        additionalAttributes.put(key, value);
    }

    @JsonAnySetter
    public void captureUnexpectedField(String field, Object ignored) {
        if (!ALLOWED_FIELDS.contains(field)) {
            unexpectedFields.add(field);
        }
    }
}
