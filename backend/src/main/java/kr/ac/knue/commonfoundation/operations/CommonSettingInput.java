package kr.ac.knue.commonfoundation.operations;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommonSettingInput {
    @NotBlank
    private String settingKey;
    @NotBlank
    private String settingValue;
    private String settingUnit;
    private final Map<String, Object> unexpectedFields = new LinkedHashMap<>();

    public CommonSettingInput() {
    }

    public CommonSettingInput(String settingKey, String settingValue, String settingUnit, Long userId, String businessScope) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.settingUnit = settingUnit;
        if (userId != null) unexpectedFields.put("userId", userId);
        if (businessScope != null) unexpectedFields.put("businessScope", businessScope);
    }

    @JsonAnySetter
    public void putUnexpected(String field, Object value) {
        unexpectedFields.put(field, value);
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public String getSettingUnit() { return settingUnit; }
    public void setSettingUnit(String settingUnit) { this.settingUnit = settingUnit; }
    public Map<String, Object> getUnexpectedFields() { return unexpectedFields; }
}
