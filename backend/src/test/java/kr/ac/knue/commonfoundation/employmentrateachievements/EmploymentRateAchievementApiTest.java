package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.*;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Database-free tests of all eight real controller routes and the actual exception advice. */
class EmploymentRateAchievementApiTest {
    private static final String BASE = "/api/business/employment-rate-achievements";
    private final EmploymentRateAchievementService service = mock(EmploymentRateAchievementService.class);
    private final EmploymentRateExcelService excel = mock(EmploymentRateExcelService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateAchievementController(service, excel))
                .setControllerAdvice(new GlobalExceptionHandler(), new EducationAchievementConflictAdvice(),
                        new EmploymentRateAchievementAdvice()).build();
    }

    @Test
    void allReadRoutesAndBinaryDownloadDelegateWithScopeAndCorrelation() throws Exception {
        CurrentUser r01 = EmploymentRateAchievementServiceTest.user("R01");
        when(service.list(0, 20, r01)).thenReturn(Map.of("achievements", List.of(), "totalElements", 0));
        when(service.detail(1L, r01)).thenReturn(Map.of("achievementId", 1, "attachmentIds", List.of()));
        when(service.downloadRows(0, 20, r01)).thenReturn(List.of(Map.of("employeeNo", "E101")));
        when(excel.download(anyList())).thenReturn(new byte[] {80, 75});
        mvc.perform(get(BASE).requestAttr("currentUser", r01).header("X-Request-Id", " REQ "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.requestId").value("REQ"));
        mvc.perform(get(BASE + "/1").requestAttr("currentUser", r01))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attachmentIds").isArray());
        mvc.perform(get(BASE + "/download").requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andExpect(content().bytes(new byte[] {80, 75}));
        verify(excel).download(List.of(Map.of("employeeNo", "E101")));
    }

    @Test
    void createUpdateAndTypedConflictUseSameRequestId() throws Exception {
        CurrentUser r01 = EmploymentRateAchievementServiceTest.user("R01");
        when(service.create(any(), eq(r01), eq("REQ"))).thenReturn(Map.of("achievement", Map.of("achievementId", 1)));
        mvc.perform(post(BASE).requestAttr("currentUser", r01).header("X-Request-Id", "REQ")
                .contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(1));
        when(service.update(eq(1L), any(), eq(r01), eq("REQ"))).thenThrow(
                new EducationAchievementConflictException("CONFIRMED_DATA_LOCKED", "확정", "REQ"));
        mvc.perform(put(BASE + "/1").requestAttr("currentUser", r01).header("X-Request-Id", "REQ")
                .contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ"));
    }

    @Test
    void uploadCallsExistingExcelUploadAndBulkRoutesAreWired() throws Exception {
        CurrentUser r07 = EmploymentRateAchievementServiceTest.user("R07");
        when(excel.upload(any(), eq(r07), eq("REQ"))).thenReturn(
                new kr.ac.knue.commonfoundation.excel.ExcelUploadResult(
                        "UP", "EMPLOYMENT_RATE_ACHIEVEMENT", "test.xlsx", "ERROR",
                        1, 0, 1, 0, 0, List.of()));
        mvc.perform(multipart(BASE + "/excel-uploads")
                .file(new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[] {80, 75}))
                .requestAttr("currentUser", r07).header("X-Request-Id", "REQ"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.uploadId").value("UP"))
                .andExpect(jsonPath("$.data.validationStatus").value("ERROR"))
                .andExpect(jsonPath("$.data.savedCount").value(0));
        verify(excel).upload(any(), eq(r07), eq("REQ"));
        when(service.createBulk(any(), eq(r07), anyString())).thenThrow(new ConflictException("정책 미승인"));
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", r07)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\",\"targetCondition\":{}}"))
                .andExpect(status().isConflict());
        when(service.job("JOB", r07)).thenReturn(Map.of("jobId", "JOB", "items", List.of()));
        mvc.perform(get(BASE + "/bulk-jobs/JOB").requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.jobId").value("JOB"));
    }

    @Test
    void endpointRolesPreventAllUnauthorizedDelegation() throws Exception {
        CurrentUser r07 = EmploymentRateAchievementServiceTest.user("R07");
        CurrentUser r02 = EmploymentRateAchievementServiceTest.user("R02");
        CurrentUser r01 = EmploymentRateAchievementServiceTest.user("R01");
        mvc.perform(get(BASE).requestAttr("currentUser", r07)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/1").requestAttr("currentUser", r07)).andExpect(status().isForbidden());
        mvc.perform(post(BASE).requestAttr("currentUser", r02).contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/1").requestAttr("currentUser", r02).contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/download").requestAttr("currentUser", EmploymentRateAchievementServiceTest.user("R08")))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(BASE + "/excel-uploads").file(new MockMultipartFile("file", new byte[] {1}))
                .requestAttr("currentUser", r01)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", r01)
                .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationYear\":\"2026\",\"actionType\":\"DELETE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/bulk-jobs/JOB").requestAttr("currentUser", r01)).andExpect(status().isForbidden());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, excel);
    }

    @Test
    void minimalFilterMappingCoversEverySubroute() throws Exception {
        AuthService auth = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        CurrentUser r07 = EmploymentRateAchievementServiceTest.user("R07");
        when(auth.currentUser("session")).thenReturn(r07);
        when(permissions.canAccess(101L, r07.roles(), "/faculty/employment-rate-achievements")).thenReturn(true);
        AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, new ObjectMapper());
        for (String suffix : List.of("", "/1", "/download", "/excel-uploads", "/bulk-jobs", "/bulk-jobs/JOB")) {
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET", BASE + suffix);
            request.setCookies(new jakarta.servlet.http.Cookie(AuthController.SESSION_COOKIE, "session"));
            var chain = mock(jakarta.servlet.FilterChain.class);
            filter.doFilter(request, new org.springframework.mock.web.MockHttpServletResponse(), chain);
            verify(chain).doFilter(any(), any());
        }
        verify(permissions, times(6)).canAccess(101L, r07.roles(), "/faculty/employment-rate-achievements");
    }

    @Test
    void malformedDateIs400AndMissingDetailIs404() throws Exception {
        CurrentUser r01 = EmploymentRateAchievementServiceTest.user("R01");
        mvc.perform(post(BASE).requestAttr("currentUser", r01).header("X-Request-Id", "REQ-BAD")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"managementItemCode\":\"ITEM\",\"achievementDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-BAD"));
        verify(service, never()).create(any(), any(), any());
        when(service.detail(99L, r01)).thenThrow(new NotFoundException("없음"));
        mvc.perform(get(BASE + "/99").requestAttr("currentUser", r01))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiredFieldErrorsPrecedeTargetLookupAndNameEachField() throws Exception {
        var mapper = mock(EmploymentRateAchievementMapper.class);
        var guards = mock(kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper.class);
        var functions = mock(kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionMapper.class);
        var actual = new EmploymentRateAchievementService(mapper, guards, functions, new ObjectMapper());
        var http = MockMvcBuilders.standaloneSetup(new EmploymentRateAchievementController(actual, excel))
                .setControllerAdvice(new GlobalExceptionHandler(), new EmploymentRateAchievementAdvice()).build();
        CurrentUser r01 = EmploymentRateAchievementServiceTest.user("R01");
        for (var request : List.of(post(BASE), put(BASE + "/999"))) {
            http.perform(request.requestAttr("currentUser", r01).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"))
                    .andExpect(jsonPath("$.error.fields[1].field").value("achievementDate"));
        }
        http.perform(post(BASE + "/bulk-jobs")
                .requestAttr("currentUser", EmploymentRateAchievementServiceTest.user("R07"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("evaluationYear"))
                .andExpect(jsonPath("$.error.fields[1].field").value("actionType"));
        verifyNoInteractions(mapper, guards, functions);
    }

    @Test
    void r07CommonExcelReadsAndCommitUseFacultyMenuButOtherWritesDoNot() throws Exception {
        AuthService auth = mock(AuthService.class);
        EffectivePermissionService permissions = mock(EffectivePermissionService.class);
        CurrentUser r07 = EmploymentRateAchievementServiceTest.user("R07");
        when(auth.currentUser("session")).thenReturn(r07);
        when(permissions.canAccess(101L, r07.roles(), "/faculty/employment-rate-achievements")).thenReturn(true);
        AuthenticationFilter filter = new AuthenticationFilter(auth, permissions, new ObjectMapper());
        for (String path : List.of("/api/admin/excel-upload-templates",
                "/api/admin/excel-upload-templates/ER-TPL/file", "/api/admin/excel-upload-histories",
                "/api/admin/excel-upload-errors", "/api/admin/excel-upload-errors/download",
                "/api/admin/excel-uploads/ER-UP-owned/commit")) {
            var request = new org.springframework.mock.web.MockHttpServletRequest(
                    path.endsWith("commit") ? "POST" : "GET", path);
            request.addParameter("businessType", "EMPLOYMENT_RATE_ACHIEVEMENT");
            request.addParameter("uploadId", "ER-UP-owned");
            request.setCookies(new jakarta.servlet.http.Cookie(AuthController.SESSION_COOKIE, "session"));
            var chain = mock(jakarta.servlet.FilterChain.class);
            filter.doFilter(request, new org.springframework.mock.web.MockHttpServletResponse(), chain);
            verify(chain).doFilter(any(), any());
        }
        var forbidden = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/admin/excel-upload-templates");
        forbidden.setCookies(new jakarta.servlet.http.Cookie(AuthController.SESSION_COOKIE, "session"));
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        filter.doFilter(forbidden, response, chain);
        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(chain);
    }

    private static String input() {
        return "{\"managementItemCode\":\"ITEM\",\"achievementDate\":\"2026-04-10\",\"attachmentIds\":[]}";
    }
}
