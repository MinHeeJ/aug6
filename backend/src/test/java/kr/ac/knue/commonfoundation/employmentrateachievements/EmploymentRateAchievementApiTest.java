package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
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

/** MockMvc contract tests for employment-rate individual and R07 bulk endpoints. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private EmploymentRateAchievementService service;
    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E107", "담당자", List.of("R07"), List.of());

    @Test
    void readsApprovedOpenApiAndCreatesEmploymentRateAchievement() throws Exception {
        String openApi = StreamUtils.copyToString(new ClassPathResource("contracts/openapi.yaml")
        .getInputStream(),
        StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(openApi)
        .contains("/api/business/employment-rate-achievements:")
        .contains("operationId: uploadEmploymentRateAchievementsExcel");
        when(service.create(any(), eq(r01), eq("ER-TRACE"))).thenReturn(row());
        mockMvc.perform(post("/api/business/employment-rate-achievements")
        .requestAttr("currentUser",
        r01)
        .cookie(cookie())
        .header("X-Request-Id",
        "ER-TRACE")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"managementItemCode\":\"EMPLOYMENT_RATE\",\"achievementDate\":\"2026-03-10\",\"achievementName\":\"취업률 제고\"}"))
                .andExpect(status()
        .isOk())
        .andExpect(jsonPath("$.data.managementItemCode")
        .value("EMPLOYMENT_RATE"))
        .andExpect(jsonPath("$.meta.requestId")
        .value("ER-TRACE"));
        verify(service).create(any(), eq(r01), eq("ER-TRACE"));
    }

    @Test
    void rejectsR01ExcelUploadButReturnsR07ValidationResult() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file",
        "employment-rate.csv",
        "text/csv",
        "employeeNo,managementItemCode,achievementDate,achievementName,attachmentRef\nE101,EMPLOYMENT_RATE,2026-03-10,취업률 제고,".getBytes(StandardCharsets.UTF_8));
        when(service.upload(any(),
        eq(r07)))
        .thenReturn(new EmploymentRateExcelUploadResult("ER-UP-1",
        "employment-rate.csv",
        1,
        1,
        0,
        1,
        List.of()));
        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
        .file(file)
        .requestAttr("currentUser",
        r07)
        .cookie(cookie()))
        .andExpect(status()
        .isOk())
        .andExpect(jsonPath("$.data.persistedCount")
        .value(1));
        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
        .file(file)
        .requestAttr("currentUser",
        r01)
        .cookie(cookie()))
        .andExpect(status()
        .isForbidden());
    }

    @Test
    void exposesBulkPolicyConflictAndOwnJobLookupRoute() throws Exception {
        org.mockito.Mockito.doThrow(new kr.ac.knue.commonfoundation.common.api.ConflictException("OQ-83-01"))
        .when(service)
        .createBulkJob(any(),
        eq(r07));
        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
        .requestAttr("currentUser",
        r07)
        .cookie(cookie())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\",\"targetCondition\":{}}"))
                .andExpect(status().isConflict());
        when(service.getBulkJob("JOB-1",
        r07))
        .thenReturn(new EmploymentRateBulkJobResult("JOB-1",
        "2026",
        "GENERATE",
        "COMPLETED",
        1,
        0,
        List.of()));
        mockMvc.perform(get("/api/business/employment-rate-achievements/bulk-jobs/JOB-1")
        .requestAttr("currentUser",
        r07)
        .cookie(cookie()))
        .andExpect(status()
        .isOk())
        .andExpect(jsonPath("$.data.jobId")
        .value("JOB-1"));
    }

    private EmploymentRateAchievementRow row() { return new EmploymentRateAchievementRow(1L,
        "ER-1",
        101L,
        "faculty",
        "2026",
        "EMPLOYMENT_RATE",
        LocalDate.parse("2026-03-10"),
        "취업률 제고",
        "DRAFT",
        List.of(),
        LocalDateTime.parse("2026-03-10T09:00:00"),
        LocalDateTime.parse("2026-03-10T09:00:00")); }
    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST"); }
}
