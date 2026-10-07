package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Database-free transport tests exercise every feature-owned Excel route and its real advice. */
class EmploymentRateExcelControllerTest {
    private static final String BASE = "/api/business/employment-rate-achievements/excel-uploads";
    private final EmploymentRateExcelService service = mock(EmploymentRateExcelService.class);
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E107", "담당자", List.of("R07"), List.of());
    private final EmploymentRateWorkbookCodec codec = new EmploymentRateWorkbookCodec();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateExcelController(service))
                .setControllerAdvice(new EmploymentRateExceptionHandler()).build();
    }

    @Test
    void invalidUploadReturns400WithRetainedIdentityAndZeroSavedDiagnostics() throws Exception {
        var error = new EmploymentRateExcelService.Error(3, "실적명", "", "INVALID_NAME", "필수", "수정");
        when(service.validateExcelUpload(any(), eq(r07), eq("req"))).thenReturn(
                new EmploymentRateExcelService.UploadResult("up", "rates.xlsx", 1, 0, 1, 0,
                        List.of(error), List.of(), BASE + "/up/errors/download"));
        mvc.perform(upload().requestAttr("currentUser", r07).header("X-Request-Id", "req"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.uploadId").value("up"))
                .andExpect(jsonPath("$.data.savedCount").value(0))
                .andExpect(jsonPath("$.data.errors[0].rowNumber").value(3))
                .andExpect(jsonPath("$.data.errorDownloadUrl").value(BASE + "/up/errors/download"))
                .andExpect(jsonPath("$.error.fields[0].field").value("rows[3].실적명"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.meta.requestId").value("req"));
    }

    @Test
    void normalUploadIsOnlyValidatedAndPropagatesTrimmedRequestId() throws Exception {
        when(service.validateExcelUpload(any(), eq(r07), eq("req"))).thenReturn(
                new EmploymentRateExcelService.UploadResult("up", "rates.xlsx", 2, 2, 0, 0,
                        List.of(), List.of(), null));
        mvc.perform(upload().requestAttr("currentUser", r07).header("X-Request-Id", " req "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.savedCount").value(0))
                .andExpect(jsonPath("$.meta.requestId").value("req"));
        verify(service).validateExcelUpload(any(), eq(r07), eq("req"));
        verify(service, never()).commitExcelUpload(any(), any(), anyBoolean(), any());
    }

    @Test
    void templateAndRetainedErrorDownloadsReturnActualXlsxBytesAndAttachmentHeaders() throws Exception {
        byte[] template = codec.write(List.of(EmploymentRateExcelService.COLUMNS));
        byte[] errors = codec.write(List.of(List.of("행", "오류"), List.of("3", "필수")));
        when(service.downloadExcelTemplate(r07)).thenReturn(new ExcelDownloadFile("template.xlsx", EmploymentRateWorkbookCodec.MIME, template));
        when(service.downloadExcelErrors("up", r07)).thenReturn(new ExcelDownloadFile("errors.xlsx", EmploymentRateWorkbookCodec.MIME, errors));
        mvc.perform(get(BASE + "/template").requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbookCodec.MIME))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("template.xlsx")))
                .andExpect(content().bytes(template));
        mvc.perform(get(BASE + "/up/errors/download").requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbookCodec.MIME))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("errors.xlsx")))
                .andExpect(content().bytes(errors));
    }

    @Test
    void listsRetainedErrorsAndOwnerHistoriesInCorrelatedEnvelope() throws Exception {
        when(service.listExcelErrors("up", r07)).thenReturn(List.of(
                new EmploymentRateExcelService.Error(4, "교번", "OTHER", "TARGET_OUT_OF_SCOPE", "소속", "수정")));
        when(service.listExcelHistories(r07)).thenReturn(List.of(Map.of("uploadId", "up", "savedCount", 0)));
        mvc.perform(get(BASE + "/up/errors").requestAttr("currentUser", r07).header("X-Request-Id", "req"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].errorCode").value("TARGET_OUT_OF_SCOPE"))
                .andExpect(jsonPath("$.data[0].inputValue").value("OTHER"))
                .andExpect(jsonPath("$.meta.requestId").value("req"));
        mvc.perform(get(BASE + "/histories").requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].uploadId").value("up"))
                .andExpect(jsonPath("$.data[0].savedCount").value(0))
                .andExpect(jsonPath("$.meta.requestId").isNotEmpty());
    }

    @Test
    void confirmedCommitReturnsSavedCountAndDelegatesExactIdentity() throws Exception {
        when(service.commitExcelUpload("up", r07, true, "req"))
                .thenReturn(new EmploymentRateExcelService.CommitResult("up", 2, List.of()));
        mvc.perform(commit("{\"confirmed\":true}").requestAttr("currentUser", r07).header("X-Request-Id", "req"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("req"));
        verify(service).commitExcelUpload("up", r07, true, "req");
    }

    @Test
    void missingFalseOrNullConfirmationCannotInvokeCommit() throws Exception {
        for (String body : List.of("", "{}", "{\"confirmed\":false}", "{\"confirmed\":null}")) {
            mvc.perform(commit(body).requestAttr("currentUser", r07).header("X-Request-Id", "req"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("CONFIRMATION_REQUIRED")))
                    .andExpect(jsonPath("$.meta.requestId").value("req"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void serviceConflictsAndUnownedDownloadsUseFeatureAdviceWithoutInternalLeakage() throws Exception {
        when(service.commitExcelUpload("up", r07, true, "req")).thenThrow(new ConflictException("DUPLICATE: 재검증"));
        mvc.perform(commit("{\"confirmed\":true}").requestAttr("currentUser", r07).header("X-Request-Id", "req"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"));
        when(service.downloadExcelErrors("other", r07)).thenThrow(new NotFoundException("업로드 없음"));
        mvc.perform(get(BASE + "/other/errors/download").requestAttr("currentUser", r07))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        when(service.listExcelHistories(r07)).thenThrow(new IllegalStateException("database-password-secret"));
        String response = mvc.perform(get(BASE + "/histories").requestAttr("currentUser", r07))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("database-password-secret");
    }

    @Test
    void everyExcelRouteRejectsAnonymousFacultyAndAdministratorBeforeServiceCalls() throws Exception {
        for (int route = 0; route < 6; route++) {
            mvc.perform(route(route)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
            for (String role : List.of("R01", "R09")) {
                CurrentUser denied = new CurrentUser(1L, "denied", "E1", "거부", List.of(role), List.of());
                mvc.perform(route(route).requestAttr("currentUser", denied)).andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
            }
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingMultipartFileAndMalformedCommitReturn400WithoutMutation() throws Exception {
        mvc.perform(multipart(BASE).requestAttr("currentUser", r07)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        mvc.perform(commit("{broken").requestAttr("currentUser", r07)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    private MockHttpServletRequestBuilder upload() {
        return multipart(BASE).file(new MockMultipartFile("file", "rates.xlsx", EmploymentRateWorkbookCodec.MIME,
                codec.write(List.of(EmploymentRateExcelService.COLUMNS))));
    }

    private MockHttpServletRequestBuilder commit(String body) {
        return post(BASE + "/up/commit").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder route(int route) {
        return switch (route) {
            case 0 -> get(BASE + "/template");
            case 1 -> upload();
            case 2 -> commit("{\"confirmed\":true}");
            case 3 -> get(BASE + "/histories");
            case 4 -> get(BASE + "/up/errors");
            case 5 -> get(BASE + "/up/errors/download");
            default -> throw new IllegalArgumentException();
        };
    }
}
