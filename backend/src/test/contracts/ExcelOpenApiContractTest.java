import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelOpenApiContractTest {
    private final String openApi;

    ExcelOpenApiContractTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void uploadTemplateOperationsDeclareSessionRoleValidationAndDownloadContract() {
        assertOperationContract("/api/admin/excel-upload-templates:", "get:", "operationId: listUploadTemplates",
                "businessType", "effectiveDate", "REQ-595", "REQ-599");
        assertOperationContract("/api/admin/excel-upload-templates:", "post:", "operationId: saveUploadTemplate",
                "UploadTemplateSaveRequest", "rules", "requiredColumn", "codeRuleRef", "REQ-596");
        assertOperationContract("/api/admin/excel-upload-templates/{templateId}/file:", "get:", "operationId: downloadUploadTemplate",
                "application/octet-stream", "templateId", "REQ-597", "REQ-586");
    }

    @Test
    void excelUploadOperationsDeclareMultipartCommitConflictAndSideEffects() {
        assertOperationContract("/api/admin/excel-uploads:", "post:", "operationId: createExcelUpload",
                "multipart/form-data", "ExcelUploadRequest", "excel_upload_files", "excel_upload_errors", "REQ-601", "REQ-603");
        assertOperationContract("/api/admin/excel-uploads/{uploadId}/commit:", "post:", "operationId: commitExcelUpload",
                "uploadId", "'409'", "VALIDATED -> COMMITTED", "REQ-603", "REQ-604");
    }

    @Test
    void historyErrorsAndDownloadOperationsExposeReadOnlyAndFileSecurityContracts() {
        assertOperationContract("/api/admin/excel-upload-histories:", "get:", "operationId: listExcelUploadHistories",
                "uploadId", "originalFileName", "REQ-608", "REQ-610");
        assertOperationContract("/api/admin/excel-upload-errors:", "get:", "operationId: listExcelUploadErrors",
                "uploadId", "REQ-612", "REQ-616");
        assertOperationContract("/api/admin/excel-upload-errors/download:", "get:", "operationId: downloadExcelUploadErrors",
                "application/octet-stream", "REQ-614", "REQ-586");
        assertOperationContract("/api/admin/excel-downloads:", "post:", "operationId: createExcelDownload",
                "ExcelDownloadRequest", "outputType", "queryCondition", "REQ-617", "REQ-619");
    }

    private void assertOperationContract(String path, String method, String operationId, String... requiredSnippets) {
        int pathIndex = openApi.indexOf(path);
        assertThat(pathIndex).as(path + " path must exist in durable OpenAPI fixture").isGreaterThanOrEqualTo(0);
        int methodIndex = openApi.indexOf(method, pathIndex);
        assertThat(methodIndex).as(method + " method must exist after " + path).isGreaterThanOrEqualTo(pathIndex);
        String operationBlock = openApi.substring(methodIndex, Math.min(openApi.length(), methodIndex + 5200));
        assertThat(operationBlock)
                .contains(operationId, "'200':", "security:", "SessionCookie", "x-roles", "R09")
                .contains(requiredSnippets);
    }
}
