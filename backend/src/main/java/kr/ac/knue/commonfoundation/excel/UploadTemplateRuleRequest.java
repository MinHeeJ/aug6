package kr.ac.knue.commonfoundation.excel;

public class UploadTemplateRuleRequest {
    private String ruleId;
    private String requiredColumn;
    private Integer columnOrder;
    private String codeRuleRef;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRequiredColumn() { return requiredColumn; }
    public void setRequiredColumn(String requiredColumn) { this.requiredColumn = requiredColumn; }
    public Integer getColumnOrder() { return columnOrder; }
    public void setColumnOrder(Integer columnOrder) { this.columnOrder = columnOrder; }
    public String getCodeRuleRef() { return codeRuleRef; }
    public void setCodeRuleRef(String codeRuleRef) { this.codeRuleRef = codeRuleRef; }
}
