package kr.ac.knue.commonfoundation.acceptance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementController;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementService;
import kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateAchievementViews;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementController;
import kr.ac.knue.commonfoundation.employmentrateimprovements.EmploymentRateImprovementService;
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

/**
 * Verifies the BASIC-83 endpoints whose controller boundary must reject roles
 * before an application service can execute a write or operational action.
 */
@WebMvcTest({
        EmploymentRateImprovementController.class,
        EmploymentRateAchievementController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BusinessAchievementAuthorizationContractTest {
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmploymentRateImprovementService improvementService;

    @MockBean
    private EmploymentRateAchievementService achievementService;

    private final CurrentUser r01 = currentUser("R01");
    private final CurrentUser r02 = currentUser("R02");
    private final CurrentUser r07 = currentUser("R07");

    @Test
    void approvedOpenApiFixtureDeclaresAllBusinessAchievementOperations() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("operationId: listEmploymentRateImprovements")
                .contains("operationId: createEmploymentRateImprovement")
                .contains("operationId: getEmploymentRateImprovement")
                .contains("operationId: updateEmploymentRateImprovement")
                .contains("operationId: listEmploymentRateAchievements")
                .contains("operationId: createEmploymentRateAchievement")
                .contains("operationId: getEmploymentRateAchievement")
                .contains("operationId: updateEmploymentRateAchievement")
                .contains("operationId: downloadEmploymentRateAchievements")
                .contains("operationId: uploadEmploymentRateAchievementsExcel")
                .contains("operationId: createEmploymentRateBulkJob")
                .contains("operationId: getEmploymentRateBulkJob");
    }

    @Test
    void readerRolesAreEnforcedBeforeImprovementAndAchievementServices() throws Exception {
        when(improvementService.list(0, 20, r02)).thenReturn(null);
        when(achievementService.list(0, 20, r02)).thenReturn(null);

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r02))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r02))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        when(achievementService.list(0, 20, r07)).thenReturn(null);
        mockMvc.perform(get("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(improvementService, never()).list(0, 20, r07);
        verify(achievementService).list(0, 20, r07);
    }

    @Test
    void writerAndExcelRolesAreEnforcedBeforeMutatingServices() throws Exception {
        mockMvc.perform(post("/api/business/employment-rate-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(improvementRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/business/employment-rate-achievements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(achievementRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        MockMultipartFile workbook = new MockMultipartFile(
                "file",
                "employment-rate.xlsx",
                XLSX_CONTENT_TYPE,
                "not-a-workbook".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/business/employment-rate-achievements/excel-uploads")
                        .file(workbook)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/business/employment-rate-achievements/bulk-jobs")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(improvementService, never()).create(any(), any(), any());
        verify(achievementService, never()).create(any(), any(), any());
        verify(achievementService, never()).upload(any(), any(), any());
        verify(achievementService, never()).requestBulkJob(any(), any(), any());
    }

    @Test
    void r07CanDownloadTheOperationalExport() throws Exception {
        when(achievementService.download(0, 20, r07)).thenReturn(
                new EmploymentRateAchievementViews.SearchResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/business/employment-rate-achievements/download")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX_CONTENT_TYPE));
    }

    private CurrentUser currentUser(String role) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(role), List.of());
    }

    private String improvementRequest() {
        return "{\"managementItemCode\":\"EMPLOYMENT_RATE\",\"achievementDate\":\"2026-04-01\"}";
    }

    private String achievementRequest() {
        return "{\"managementItemCode\":\"EMPLOYMENT_RATE\",\"achievementDate\":\"2026-04-01\"}";
    }
}
