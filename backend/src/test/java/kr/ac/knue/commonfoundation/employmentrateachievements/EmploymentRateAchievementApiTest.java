package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmploymentRateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({EmploymentRateApiAdvice.class, EmploymentRateXlsxCodec.class})
class EmploymentRateAchievementApiTest {
    private static final String PATH = "/api/business/employment-rate-achievements";
    private static final String BODY = """
            {"managementItemCode":"EMPLOYMENT_RATE_ACHIEVEMENT",
            "achievementDate":"2026-04-10","achievementName":"실적"}
            """;
    @Autowired MockMvc mvc;
    @MockBean EmploymentRateAchievementService service;
    @MockBean EmploymentRateExcelService excel;

    private CurrentUser user(String role) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(role), List.of());
    }

    @Test
    void createReturnsPersistedAchievementAndTrace() throws Exception {
        when(service.save(isNull(), any(), any(), eq("trace-create")))
                .thenReturn(Map.of("achievement", Map.of("achievementId", 42, "achievementName", "실적")));
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "trace-create").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(42))
                .andExpect(jsonPath("$.meta.requestId").value("trace-create"));
    }

    @Test
    void listReturnsFilteredEnvelope() throws Exception {
        when(service.list(any(), any(), eq(false)))
                .thenReturn(Map.of("achievements", List.of(Map.of("achievementId", 42)), "totalElements", 1));
        mvc.perform(get(PATH).param("managementNo", "selected").requestAttr("currentUser", user("R02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        verify(service).list(argThat(p -> "selected".equals(p.get("managementNo"))), any(), eq(false));
    }

    @Test
    void detailAndUpdateUsePathIdentity() throws Exception {
        when(service.get(eq(42L), any())).thenReturn(Map.of("achievementId", 42));
        mvc.perform(get(PATH + "/42").requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(42));
        when(service.save(eq(42L), any(), any(), any())).thenReturn(Map.of("achievement", Map.of("achievementId", 42)));
        mvc.perform(put(PATH + "/42").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(42));
    }

    @Test
    void missingManagementItemReturnsObjectFields() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2026-04-10\",\"achievementName\":\"실적\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.managementItemCode").exists());
        verifyNoInteractions(service);
    }

    @Test
    void invalidDateReturnsSafe400() throws Exception {
        mvc.perform(put(PATH + "/42").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("2026-04-10", "invalid")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.achievementDate").exists());
        verifyNoInteractions(service);
    }

    @Test
    void confirmedUpdateReturnsTypedConflict() throws Exception {
        when(service.save(eq(42L), any(), any(), any()))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 확정"));
        mvc.perform(put(PATH + "/42").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
    }

    @Test
    void notFoundReturns404() throws Exception {
        when(service.get(eq(42L), any())).thenThrow(new NotFoundException("없음"));
        mvc.perform(get(PATH + "/42").requestAttr("currentUser", user("R01")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/42"})
    void r07CannotReadIndividualData(String suffix) throws Exception {
        mvc.perform(get(PATH + suffix).requestAttr("currentUser", user("R07")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void r02CannotWrite() throws Exception {
        mvc.perform(post(PATH).requestAttr("currentUser", user("R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(PATH + "/42").requestAttr("currentUser", user("R02"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousCannotList() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPaginationRejected() throws Exception {
        mvc.perform(get(PATH).param("pageSize", "3").requestAttr("currentUser", user("R01")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.pageSize").exists());
    }

    @Test
    void downloadIsRealXlsx() throws Exception {
        when(service.list(any(), any(), eq(true)))
                .thenReturn(Map.of("achievements", List.of(Map.of("employeeNo", "E0101", "achievementName", "실적"))));
        byte[] bytes = mvc.perform(get(PATH + "/download").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(new EmploymentRateXlsxCodec().read(bytes).get(1).get(3))
                .isEqualTo("실적");
    }

    @Test
    void bulkPolicyConflictAndMissingYear() throws Exception {
        doThrow(new ConflictException("BULK_POLICY_NOT_APPROVED: 미승인"))
                .when(service).createBulk(any(), any());
        mvc.perform(post(PATH + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("BULK_POLICY_NOT_APPROVED"));
        mvc.perform(post(PATH + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields.evaluationYear").exists());
    }

    @Test
    void jobReturnsPreservedResults() throws Exception {
        when(service.job(eq("selected-job"), any()))
                .thenReturn(Map.of("jobId", "selected-job", "processedCount", 2, "unprocessedCount", 1, "items", List.of()));
        mvc.perform(get(PATH + "/bulk-jobs/selected-job").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.processedCount").value(2));
    }

    @Test
    void uploadReturnsValidationOnly() throws Exception {
        when(excel.upload(any(), any(), any())).thenReturn(Map.of("uploadId", "selected", "savedCount", 0, "errorCount", 1));
        mvc.perform(multipart(PATH + "/excel-uploads")
                        .file(new MockMultipartFile("file", "rates.xlsx", "application/octet-stream", new byte[]{1}))
                        .requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(0));
    }

    @Test
    void uploadRequiresFile() throws Exception {
        mvc.perform(multipart(PATH + "/excel-uploads").requestAttr("currentUser", user("R07")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/bulk-jobs/selected", "/excel-uploads/template", "/excel-uploads/histories",
            "/excel-uploads/selected/errors", "/excel-uploads/selected/errors/download"})
    void excelAndJobReadsRejectFaculty(String suffix) throws Exception {
        mvc.perform(get(PATH + suffix).requestAttr("currentUser", user("R01"))).andExpect(status().isForbidden());
        verifyNoInteractions(excel, service);
    }

    @Test
    void excelAndBulkCommandsRejectFaculty() throws Exception {
        mvc.perform(multipart(PATH + "/excel-uploads")
                        .file(new MockMultipartFile("file", "rates.xlsx", "application/octet-stream", new byte[]{1}))
                        .requestAttr("currentUser", user("R01"))).andExpect(status().isForbidden());
        mvc.perform(post(PATH + "/excel-uploads/selected/commit").requestAttr("currentUser", user("R01")))
                .andExpect(status().isForbidden());
        mvc.perform(post(PATH + "/bulk-jobs").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationYear\":\"2026\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminBypassIsPreserved() throws Exception {
        when(service.list(any(), any(), eq(false))).thenReturn(Map.of("achievements", List.of()));
        mvc.perform(get(PATH).requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
    }

    @Test
    void unexpectedErrorsDoNotLeakSqlOrSecrets() throws Exception {
        when(service.get(any(), any())).thenThrow(new RuntimeException("password SQL SELECT secret"));
        mvc.perform(get(PATH + "/42").requestAttr("currentUser", user("R01")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
}
