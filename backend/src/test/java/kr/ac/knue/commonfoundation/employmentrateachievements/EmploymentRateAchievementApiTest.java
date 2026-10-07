package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Uses the nearest education controller's MVC setup; service rules and persistence are tested separately. */
@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmploymentRateAchievementApiTest {
    private static final String BASE = "/api/business/employment-rate-achievements";
    private static final String BODY = """
            {"managementItemCode":"employment-rate-achievements","achievementDate":"2026-04-10","achievementName":"취업 지원"}
            """;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private EmploymentRateAchievementService service;

    private CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    @Test
    void listAndDetailKeepEnvelopeAndSelectedIdentity() throws Exception {
        Map<String, Object> row = Map.of("achievementId", 10, "achievementName", "취업 지원");
        when(service.list(any(), any())).thenReturn(Map.of("achievements", List.of(row), "totalElements", 1));
        when(service.detail(eq(10L), any())).thenReturn(row);
        mvc.perform(get(BASE).requestAttr("currentUser", user("R01")).header("X-Request-Id", "list-trace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("list-trace"));
        mvc.perform(get(BASE + "/10").requestAttr("currentUser", user("R02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(10));
    }

    @Test
    void createAndPutHaveDistinctMethodsAndPreserveWarning() throws Exception {
        Map<String, Object> result = Map.of("achievement", Map.of("achievementId", 10), "occurredDateWarning", true);
        when(service.create(any(), any(), any())).thenReturn(result);
        when(service.update(eq(10L), any(), any(), any())).thenReturn(result);
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(10));
        verify(service).create(any(), any(), any());
        verify(service).update(eq(10L), any(), any(), any());
    }

    @Test
    void missingItemAndMalformedDateAre400WithoutServiceCalls() throws Exception {
        mvc.perform(post(BASE).requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"not-a-date\",\"managementItemCode\":\"item\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void roleAdmissionDoesNotGrantR07OrdinaryReadOrR02Write() throws Exception {
        mvc.perform(get(BASE).requestAttr("currentUser", user("R07"))).andExpect(status().isForbidden());
        mvc.perform(post(BASE).requestAttr("currentUser", user("R02")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void unknownDetailAndConfirmedUpdateExposeSafeStatus() throws Exception {
        when(service.detail(eq(10L), any())).thenThrow(new NotFoundException("실적 없음"));
        when(service.update(eq(10L), any(), any(), any())).thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));
        mvc.perform(get(BASE + "/10").requestAttr("currentUser", user("R01")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(put(BASE + "/10").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value("CONFIRMED_DATA_LOCKED"));
    }

    @Test
    void downloadIsRealXlsxAndAllowsR07() throws Exception {
        byte[] bytes = EmploymentRateXlsxCodec.write(List.of(List.of("실적명"), List.of("취업 지원")));
        when(service.download(any(), any())).thenReturn(bytes);
        mvc.perform(get(BASE + "/download").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateXlsxCodec.CONTENT_TYPE))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void bulkPolicyConflictAndMissingYearHaveDifferentContracts() throws Exception {
        when(service.createJob(any(), any())).thenThrow(new ConflictException("POLICY_NOT_APPROVED"));
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'evaluationYear')]").isNotEmpty());
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isConflict());
        verify(service, times(1)).createJob(any(), any());
    }

    @Test
    void jobReturnsPersistedCountsOrNotFound() throws Exception {
        when(service.job(eq("selected-job"), any())).thenReturn(Map.of("jobId", "selected-job", "processedCount", 0,
                "unprocessedCount", 1, "items", List.of(Map.of("processedYn", "N"))));
        mvc.perform(get(BASE + "/bulk-jobs/selected-job").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.unprocessedCount").value(1))
                .andExpect(jsonPath("$.data.items[0].processedYn").value("N"));
        when(service.job(eq("missing"), any())).thenThrow(new NotFoundException("작업 없음"));
        mvc.perform(get(BASE + "/bulk-jobs/missing").requestAttr("currentUser", user("R07")))
                .andExpect(status().isNotFound());
    }

    @Test
    void multipartUploadStagesButDoesNotCommitAndRejectsNonOperator() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "input.xlsx", EmploymentRateXlsxCodec.CONTENT_TYPE,
                EmploymentRateXlsxCodec.write(List.of(List.of("교번"), List.of("E0101"))));
        when(service.upload(any(), any(), any())).thenReturn(Map.of("uploadId", "staged-upload", "savedCount", 0));
        mvc.perform(multipart(BASE + "/excel-uploads").file(file).requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(0));
        mvc.perform(multipart(BASE + "/excel-uploads").file(file).requestAttr("currentUser", user("R01")))
                .andExpect(status().isForbidden());
        verify(service, never()).commit(any(), any(), any());
    }

    @Test
    void administratorOverrideAndUnauthenticatedAreExplicit() throws Exception {
        when(service.list(any(), any())).thenReturn(Map.of("achievements", List.of(), "totalElements", 0));
        mvc.perform(get(BASE).requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }
}
