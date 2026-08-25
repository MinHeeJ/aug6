package kr.ac.knue.commonfoundation.helpcontents;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public class HelpContentSaveRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "businessDescription", "inputCriteria", "faq", "contact", "changeReason");

    @NotBlank(message = "업무 설명을 입력하세요.")
    private String businessDescription;

    @NotBlank(message = "입력 기준을 입력하세요.")
    private String inputCriteria;

    @Size(max = 4000, message = "FAQ는 4000자 이하여야 합니다.")
    private String faq;

    @Size(max = 200, message = "연락처는 200자 이하여야 합니다.")
    private String contact;

    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;

    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getBusinessDescription() {
        return businessDescription;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }

    public String getInputCriteria() {
        return inputCriteria;
    }

    public void setInputCriteria(String inputCriteria) {
        this.inputCriteria = inputCriteria;
    }

    public String getFaq() {
        return faq;
    }

    public void setFaq(String faq) {
        this.faq = faq;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
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
