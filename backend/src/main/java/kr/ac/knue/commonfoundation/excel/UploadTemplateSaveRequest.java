package kr.ac.knue.commonfoundation.excel;

import java.util.List;

public class UploadTemplateSaveRequest {
    private String templateId;
    private String businessType;
    private String templateVersion;
    private String effectiveDate;
    private String originalFileName;
    private String changeReason;
    private List<UploadTemplateRuleRequest> rules;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public List<UploadTemplateRuleRequest> getRules() { return rules; }
    public void setRules(List<UploadTemplateRuleRequest> rules) { this.rules = rules; }
}
