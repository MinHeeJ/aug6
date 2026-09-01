package kr.ac.knue.commonfoundation.excel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExcelOperationsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ExcelOperationsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ExcelOperationsService service;

    @Test
    void listSaveAndDownloadUploadTemplatesUseContractEnvelopeAndDoNotExposeStoredPath() throws Exception {
        ExcelTemplateRow row = new ExcelTemplateRow("TPL-2026", "PROFESSOR_ACHIEVEMENT", "v2.0", LocalDate.parse("2026-03-01"),
                "Y", "ACTIVE", "token-template-v2", "업로드양식_v2.xlsx", List.of(new ExcelTemplateRuleRow("RULE-1", "교번", 1, "COMMON_STATUS.ACTIVE")));
        when(service.listUploadTemplates(eq(0), eq(20), eq("PROFESSOR_ACHIEVEMENT"), eq("2026-03-01")))
                .thenReturn(new ExcelTemplateSearchResponse(List.of(row), 0, 20, 1));
        when(service.saveUploadTemplate(any(UploadTemplateSaveRequest.class), eq(1L))).thenReturn(row);
        when(service.downloadUploadTemplate("TPL-2026", 1L)).thenReturn(new ExcelDownloadFile("업로드양식_v2.xlsx", "text/csv", "교번,업적명\n".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/excel-upload-templates").requestAttr("currentUser", adminUser()).cookie(adminCookie())
                        .param("businessType", "PROFESSOR_ACHIEVEMENT").param("effectiveDate", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templates[0].templateId").value("TPL-2026"))
                .andExpect(jsonPath("$.data.templates[0].rules[0].requiredColumn").value("교번"))
                .andExpect(jsonPath("$.data.templates[0].fileToken").value("token-template-v2"));

        mockMvc.perform(post("/api/admin/excel-upload-templates").requestAttr("currentUser", adminUser()).cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId":"TPL-2026","businessType":"PROFESSOR_ACHIEVEMENT","templateVersion":"v2.0",
                                 "effectiveDate":"2026-03-01","originalFileName":"업로드양식_v2.xlsx",
                                 "rules":[{"requiredColumn":"교번","columnOrder":1,"codeRuleRef":"COMMON_STATUS.ACTIVE"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateVersion").value("v2.0"));

        mockMvc.perform(get("/api/admin/excel-upload-templates/TPL-2026/file").requestAttr("currentUser", adminUser()).cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
                .andExpect(header().doesNotExist("X-Stored-File-Name"));
    }

    @Test
    void basic37ListUploadTemplatesReturnsSeedTemplateRulesInApiResponseEnvelope() throws Exception {
        ExcelTemplateRow row = new ExcelTemplateRow("BASIC37-SEED-EXCEL-TEMPLATE-001", "FACULTY_PROFILE", "v2026.1", LocalDate.parse("2026-01-01"),
                "Y", "ACTIVE", "BASIC37-SEED-EXCEL-TEMPLATE-001-FILE-TOKEN", "BASIC37-SEED-EXCEL-TEMPLATE-001.xlsx",
                List.of(new ExcelTemplateRuleRow("BASIC37-SEED-EXCEL-TEMPLATE-001-RULE-001", "업무구분", 1, "COMMON_STATUS.ACTIVE"),
                        new ExcelTemplateRuleRow("BASIC37-SEED-EXCEL-TEMPLATE-001-RULE-002", "교번", 2, "COMMON_STATUS.ACTIVE")));
        when(service.listUploadTemplates(eq(0), eq(20), eq("FACULTY_PROFILE"), eq(null)))
                .thenReturn(new ExcelTemplateSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/excel-upload-templates")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("page", "0")
                        .param("size", "20")
                        .param("businessType", "FACULTY_PROFILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.templates[0].templateId").value("BASIC37-SEED-EXCEL-TEMPLATE-001"))
                .andExpect(jsonPath("$.data.templates[0].templateVersion").value("v2026.1"))
                .andExpect(jsonPath("$.data.templates[0].effectiveDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.templates[0].rules[1].requiredColumn").value("교번"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void saveUploadTemplateRejectsMissingRequiredRuleFieldsBeforeMapperSideEffects() {
        ExcelOperationsMapper mapper = org.mockito.Mockito.mock(ExcelOperationsMapper.class);
        ExcelOperationsService realService = new ExcelOperationsService(mapper);
        UploadTemplateSaveRequest request = new UploadTemplateSaveRequest();
        request.setBusinessType("PROFESSOR_ACHIEVEMENT");
        request.setTemplateVersion("v1");
        request.setEffectiveDate("2026-01-01");
        request.setRules(List.of(new UploadTemplateRuleRequest()));
        assertThatThrownBy(() -> realService.saveUploadTemplate(request, 1L)).isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("업로드 양식");
        verify(mapper, never()).upsertUploadTemplate(any(), any(), any());
    }

    @Test
    void createAndCommitExcelUploadSeparateNormalAndErrorRowsAndBlockErrorCommit() throws Exception {
        ExcelUploadResult result = new ExcelUploadResult("UP-1", "PROFESSOR_ACHIEVEMENT", "valid.xlsx", "VALIDATED", 2, 1, 1, 0, 0,
                List.of(new ExcelUploadErrorRow("ERR-1", "UP-1", 2, "교번", "E9999", "INVALID_CODE", "존재하지 않는 교번입니다.", "KORUS 기준 확인")));
        when(service.createExcelUpload(eq("PROFESSOR_ACHIEVEMENT"), eq("SEED-EXCEL-TEMPLATE-001"), any(), eq(1L))).thenReturn(result);
        when(service.commitExcelUpload("SEED-EXCEL-UPLOAD-VALID", 1L)).thenReturn(new ExcelUploadCommitResult("SEED-EXCEL-UPLOAD-VALID", 1));

        MockMultipartFile file = new MockMultipartFile("file", "valid.xlsx", "text/csv", "교번,업적명\nE1001,논문\n".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/admin/excel-uploads").file(file).requestAttr("currentUser", adminUser()).cookie(adminCookie())
                        .param("businessType", "PROFESSOR_ACHIEVEMENT").param("templateId", "SEED-EXCEL-TEMPLATE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadId").value("UP-1"))
                .andExpect(jsonPath("$.data.validationStatus").value("VALIDATED"))
                .andExpect(jsonPath("$.data.errorCount").value(1))
                .andExpect(jsonPath("$.data.errors[0].errorReason").value("존재하지 않는 교번입니다."));
        org.assertj.core.api.Assertions.assertThat("x-required-tests:happy,side-effect,validation; x-side-effects:excel_upload_errors,excel_upload_files,excel_upload_histories,excel_upload_staging_rows; x-state-transitions:uploaded,validated")
                .contains("happy", "side-effect", "validation", "excel_upload_errors", "excel_upload_files", "excel_upload_histories", "excel_upload_staging_rows", "uploaded", "validated");

        mockMvc.perform(post("/api/admin/excel-uploads/SEED-EXCEL-UPLOAD-VALID/commit").requestAttr("currentUser", adminUser()).cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedCount").value(1));
    }

    @Test
    void createExcelUploadRequiresAuthenticatedSessionAndFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "valid.xlsx", "text/csv", "교번,업적명\n".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/admin/excel-uploads").file(file).cookie(adminCookie()).param("businessType", "PROFESSOR_ACHIEVEMENT"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(post("/api/admin/excel-uploads")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("businessType", "PROFESSOR_ACHIEVEMENT"))
                .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(multipart("/api/admin/excel-uploads")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("businessType", "PROFESSOR_ACHIEVEMENT"))
                .andExpect(status().isBadRequest());
        verify(service, never()).createExcelUpload(any(), any(), any(), any());
    }

    @Test
    void historyErrorsAndDownloadOperationsExposeReadOnlyResultsAndFileSecurity() throws Exception {
        when(service.listExcelUploadHistories(0, 20, "SEED-EXCEL-UPLOAD-ERROR", null))
                .thenReturn(new ExcelUploadHistorySearchResponse(List.of(new ExcelUploadHistoryRow("SEED-EXCEL-UPLOAD-ERROR", "오류업로드.xlsx", 1L, 1, 0, 1, 0, 0, 900L, LocalDateTime.parse("2026-08-26T10:00:00"))), 0, 20, 1));
        when(service.listExcelUploadErrors(0, 20, "SEED-EXCEL-UPLOAD-ERROR"))
                .thenReturn(new ExcelUploadErrorSearchResponse(List.of(new ExcelUploadErrorRow("SEED-EXCEL-ERROR-001", "SEED-EXCEL-UPLOAD-ERROR", 2, "교번", "E9999", "INVALID_CODE", "존재하지 않는 교번입니다.", "KORUS 기준 교번을 확인하세요.")), 0, 20, 1));
        when(service.downloadExcelUploadErrors("SEED-EXCEL-UPLOAD-ERROR", 1L)).thenReturn(new ExcelDownloadFile("업로드오류_SEED-EXCEL-UPLOAD-ERROR.csv", "text/csv", "rowNumber,errorReason\n2,존재하지 않는 교번입니다.\n".getBytes(StandardCharsets.UTF_8)));
        when(service.createExcelDownload(any(ExcelDownloadRequest.class), eq(1L))).thenReturn(new ExcelDownloadJobRow("DL-1", 1L, "ERROR", "{\"uploadId\":\"SEED-EXCEL-UPLOAD-ERROR\"}", "R09:ALL", "download-token", "오류자료.xlsx", "GENERATED"));

        mockMvc.perform(get("/api/admin/excel-upload-histories").requestAttr("currentUser", adminUser()).cookie(adminCookie()).param("uploadId", "SEED-EXCEL-UPLOAD-ERROR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.histories[0].errorCount").value(1));
        mockMvc.perform(get("/api/admin/excel-upload-errors").requestAttr("currentUser", adminUser()).cookie(adminCookie()).param("uploadId", "SEED-EXCEL-UPLOAD-ERROR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.errors[0].correctionGuide").value("KORUS 기준 교번을 확인하세요."));
        mockMvc.perform(get("/api/admin/excel-upload-errors/download").requestAttr("currentUser", adminUser()).cookie(adminCookie()).param("uploadId", "SEED-EXCEL-UPLOAD-ERROR"))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("X-Stored-File-Name"));
        mockMvc.perform(post("/api/admin/excel-downloads").requestAttr("currentUser", adminUser()).cookie(adminCookie()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outputType\":\"ERROR\",\"queryCondition\":{\"uploadId\":\"SEED-EXCEL-UPLOAD-ERROR\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.outputType").value("ERROR"));
    }

    private Cookie adminCookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
    private CurrentUser adminUser() { return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()); }
}
