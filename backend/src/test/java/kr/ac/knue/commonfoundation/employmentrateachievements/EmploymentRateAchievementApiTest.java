package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** MockMvc contract coverage for the controller owning all employment-rate routes. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresEmploymentRateOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/employment-rate-achievements:")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob");
    }

    @Test
    void listReturnsApprovedEnvelopeAndRequestId() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new EmploymentRateAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-ERA-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("EMPLOYMENT"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ERA-LIST"));
    }

    @Test
    void createReturnsPersistedRecord() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-ERA-CREATE"))).thenReturn(
                new EmploymentRateAchievementService.EmploymentRateAchievementSaveResult(
                        row(), false, null));

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-ERA-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"EMPLOYMENT",
                                  "achievementDate":"2026-04-10",
                                  "achievementName":"취업률 실적"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementName").value("취업률 실적"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-ERA-CREATE"));
        verify(service).create(any(), eq(r01), eq("REQ-ERA-CREATE"));
    }

    @Test
    void updateUsesPathIdInsteadOfBodyIdentifier() throws Exception {
        when(service.update(eq(91L), any(), eq(r01), eq("REQ-ERA-UPDATE"))).thenReturn(
                new EmploymentRateAchievementService.EmploymentRateAchievementSaveResult(
                        row(), false, null));

        mockMvc.perform(put("/api/business/employment-rate-achievements/91")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-ERA-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EMPLOYMENT","achievementDate":"2026-04-11"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(91));
        verify(service).update(eq(91L), any(), eq(r01), eq("REQ-ERA-UPDATE"));
    }

    @Test
    void missingManagementItemCodeReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"achievementDate\":\"2026-04-10\"" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void rejectedExcelUploadReturnsValidationErrorWithoutACommitResponse() throws Exception {
        MockMultipartFile spreadsheet = new MockMultipartFile(
                "file",
                "employment-rate.csv",
                "text/csv",
                "교번,관리항목코드,업적발생일,실적명,첨부참조\nE9999,EMPLOYMENT,2026-04-10,취업률 실적,"
                        .getBytes(StandardCharsets.UTF_8));
        when(service.upload(any(), eq(r07))).thenThrow(new BusinessValidationException(
                "취업률 실적 Excel 검증에 실패했습니다.",
                List.of(new ValidationError("file", "오류 또는 중복 행이 있어 업무 데이터를 반영하지 않았습니다."))));

        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(spreadsheet)
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("file"));

        verify(service).upload(any(), eq(r07));
    }

    @Test
    void r01CannotRequestR07BatchJob() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2026","actionType":"GENERATE"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).requestBatchJob(any(), any());
    }

    @Test
    void downloadProvidesAttachmentForR07() throws Exception {
        when(service.list(any(), eq(r07))).thenReturn(
                new EmploymentRateAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=employment-rate-achievements.csv"));
    }

    private EmploymentRateAchievementRow row() {
        return new EmploymentRateAchievementRow(
                91L,
                "ERA-001",
                101L,
                "faculty",
                "2026",
                "EMPLOYMENT",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                "[]",
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
