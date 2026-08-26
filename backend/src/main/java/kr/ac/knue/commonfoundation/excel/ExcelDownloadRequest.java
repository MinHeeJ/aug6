package kr.ac.knue.commonfoundation.excel;

import com.fasterxml.jackson.databind.JsonNode;

public class ExcelDownloadRequest {
    private String outputType;
    private JsonNode queryCondition;

    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public JsonNode getQueryCondition() { return queryCondition; }
    public void setQueryCondition(JsonNode queryCondition) { this.queryCondition = queryCondition; }
}
