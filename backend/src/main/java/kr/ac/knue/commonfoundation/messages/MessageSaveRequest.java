package kr.ac.knue.commonfoundation.messages;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class MessageSaveRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of("messageCode", "messageType", "userMessage", "changeReason");

    @Size(max = 100, message = "메시지코드는 100자 이하여야 합니다.")
    private String messageCode;

    @NotBlank(message = "메시지 유형을 선택하세요.")
    @Size(max = 50, message = "메시지 유형은 50자 이하여야 합니다.")
    private String messageType;

    @NotBlank(message = "사용자 문구를 입력하세요.")
    @Size(max = 1000, message = "사용자 문구는 1000자 이하여야 합니다.")
    private String userMessage;

    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;

    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
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
