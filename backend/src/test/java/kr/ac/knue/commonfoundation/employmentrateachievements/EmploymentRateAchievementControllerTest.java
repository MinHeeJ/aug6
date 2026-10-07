package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP tests target the real controller and preserve role, validation and conflict contracts. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(EmploymentRateExceptionHandler.class)
class EmploymentRateAchievementControllerTest {
    @Autowired MockMvc mvc;
    @MockBean EmploymentRateAchievementService service;
    private final CurrentUser faculty = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
    private final CurrentUser operator = new CurrentUser(107L, "operator", "E107", "담당", List.of("R07"), List.of());
    private static final String BASE = "/api/business/employment-rate-achievements";
    private static final String BODY = """
            {"managementItemCode":"EMPLOYMENT_RATE","achievementDate":"2026-04-12","achievementName":"취업 실적"}
            """;

    @Test
    void listAndDetailKeepEnvelope() throws Exception {
        when(service.list(eq(faculty), anyMap(), eq(0), eq(20))).thenReturn(
                Map.of("achievements", List.of(Map.of("achievementId", 1, "achievementName", "실적")),
                        "totalElements", 1));
        when(service.detail(faculty, 1L)).thenReturn(Map.of("achievementId", 1, "achievementName", "실적"));
        mvc.perform(get(BASE).requestAttr("currentUser", faculty).header("X-Request-Id", "trace-list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].achievementId").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("trace-list"));
        mvc.perform(get(BASE + "/1").requestAttr("currentUser", faculty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementName").value("실적"));
    }

    @Test
    void createAndUpdateReturnPersistedFields() throws Exception {
        when(service.save(eq(faculty), any(), isNull(), anyString())).thenReturn(
                Map.of("achievement", Map.of("achievementId", 1, "achievementName", "취업 실적")));
        when(service.save(eq(faculty), any(), eq(1L), anyString())).thenReturn(
                Map.of("achievement", Map.of("achievementId", 1, "achievementName", "취업 실적")));
        mvc.perform(post(BASE).requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(1));
        mvc.perform(put(BASE + "/1").requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementName").value("취업 실적"));
    }

    @Test
    void validationAndMalformedDateAre400() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        mvc.perform(put(BASE + "/1").requestAttr("currentUser", faculty).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EMPLOYMENT_RATE\",\"achievementDate\":\"bad-date\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).save(any(), any(), any(), any());
    }

    @Test
    void unauthorizedWriteAndBulkAre403() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", operator).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    @Test
    void confirmedAndUnapprovedPolicyReturn409WithoutLeakingInternals() throws Exception {
        when(service.save(eq(faculty), any(), eq(1L), anyString()))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 확정 실적"));
        doThrow(new ConflictException("OQ-83-01: 미승인 정책")).when(service).createBulk(eq(operator), any());
        mvc.perform(put(BASE + "/1").requestAttr("currentUser", faculty).header("X-Request-Id", "lock-trace")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.meta.requestId").value("lock-trace"));
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\",\"targetConditionJson\":{}}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value("OQ-83-01: 미승인 정책"));
    }

    @Test
    void bulkPreviewAndSeedResultReadDatabaseResponses() throws Exception {
        when(service.preview(eq(operator), anyMap())).thenReturn(Map.of("policyApproved", false, "candidates", List.of()));
        when(service.job(operator, "seed-job")).thenReturn(Map.of("jobId", "seed-job", "seed", true, "items", List.of()));
        mvc.perform(get(BASE + "/bulk-jobs/preview").requestAttr("currentUser", operator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.policyApproved").value(false));
        mvc.perform(get(BASE + "/bulk-jobs/seed-job").requestAttr("currentUser", operator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.seed").value(true));
    }

    @Test
    void downloadHasActualXlsxBytes() throws Exception {
        byte[] bytes = new EmploymentRateWorkbookCodec().write(List.of(List.of("실적명"), List.of("실적")));
        when(service.download(eq(faculty), anyMap())).thenReturn(bytes);
        mvc.perform(get(BASE + "/download").requestAttr("currentUser", faculty))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbookCodec.MIME))
                .andExpect(content().bytes(bytes));
    }
}
