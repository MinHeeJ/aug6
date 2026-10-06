package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP plus real application-service tests; persistence and servlet filter are separate runner boundaries. */
class EmploymentRateAchievementContractTest {
    private static final String BASE = "/api/business/employment-rate-achievements";
    private static final String INPUT = """
            {"managementItemCode":"FR-032","achievementDate":"2025-04-12",
             "achievementName":"취업률 실적","attachmentIds":[]}
            """;
    private final EmploymentRateAchievementMapper mapper = mock(EmploymentRateAchievementMapper.class);
    private final EducationAchievementGuardService guards = mock(EducationAchievementGuardService.class);
    private final FunctionPermissionService permissions = mock(FunctionPermissionService.class);
    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final CurrentUser teacher = user("R01");
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        EmploymentRateAchievementService service = new EmploymentRateAchievementService(
                mapper, guards, permissions, storage, json, new EmploymentRateWorkbook());
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateAchievementController(service))
                .setControllerAdvice(new EmploymentRateInputErrorHandler(), new GlobalExceptionHandler())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(json))
                .build();
        when(guards.validateMutation(any(), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.itemRules("FR-032", "2025")).thenReturn(List.of(
                Map.of("requiredYn", "Y", "editableYn", "Y", "dataType", "TEXT")));
        when(mapper.organization(7L)).thenReturn("DEPARTMENT");
        when(mapper.visible(eq(51L), eq(7L), anyList())).thenReturn(1);
    }

    @Test
    void fixtureIsLoadedFromClasspath() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThat(stream.readAllBytes()).isNotEmpty();
        }
    }

    @Test
    void createReadListAndAuditMatchGeneratedIdentity() throws Exception {
        Map<String, Object> persisted = row("DRAFT");
        when(mapper.find(51L, false)).thenReturn(persisted);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(0);
            values.put("achievementId", 51L);
            assertThat(values.get("teacherUserId")).isEqualTo(7L);
            return null;
        }).when(mapper).insert(anyMap());
        when(mapper.list(anyMap(), eq(7L), eq(List.of("R01")))).thenReturn(List.of(persisted));
        when(mapper.count(anyMap(), eq(7L), eq(List.of("R01")))).thenReturn(1L);
        mvc.perform(post(BASE).requestAttr("currentUser", teacher).header("X-Request-Id", "trace-create")
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievement.achievementId").value(51))
                .andExpect(jsonPath("$.data.achievement.achievementDate").value("2025-04-12"))
                .andExpect(jsonPath("$.data.achievement.attachmentIds").isArray())
                .andExpect(jsonPath("$.meta.requestId").value("trace-create"));
        mvc.perform(get(BASE + "/51").requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementName").value("취업률 실적"));
        mvc.perform(get(BASE).requestAttr("currentUser", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].achievementName").value("취업률 실적"));
        ArgumentCaptor<String> snapshot = ArgumentCaptor.forClass(String.class);
        verify(mapper).history(eq(51L), eq("CREATE"), isNull(), snapshot.capture(), eq(7L), eq("trace-create"));
        assertThat(json.readTree(snapshot.getValue()).get("achievementDate").asText()).isEqualTo("2025-04-12");
        verify(mapper).initialStatus(51L, 7L);
        var ordered = inOrder(mapper);
        ordered.verify(mapper).insert(anyMap());
        ordered.verify(mapper).find(51L, false);
        ordered.verify(mapper).initialStatus(51L, 7L);
    }

    @Test
    void updateRetainsYearWarnsAndAuditsAllChangedFields() throws Exception {
        Map<String, Object> old = row("DRAFT");
        old.put("achievementName", "수정 전");
        Map<String, Object> after = row("DRAFT");
        after.put("achievementDate", LocalDate.of(2024, 12, 31));
        when(mapper.find(51L, true)).thenReturn(old);
        when(mapper.find(51L, false)).thenReturn(after);
        when(mapper.update(anyMap())).thenReturn(1);
        when(guards.validateMutation(eq(teacher), any())).thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        mvc.perform(put(BASE + "/51").requestAttr("currentUser", teacher).header("X-Request-Id", "trace-update")
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT.replace("2025-04-12", "2024-12-31")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.achievementDate").value("2024-12-31"));
        verify(guards).validateMutation(teacher,
                new EducationAchievementMutationContext(7L, "2025", LocalDate.of(2024, 12, 31)));
        ArgumentCaptor<String> before = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> next = ArgumentCaptor.forClass(String.class);
        verify(mapper).history(eq(51L), eq("UPDATE"), before.capture(), next.capture(), eq(7L), eq("trace-update"));
        assertThat(json.readTree(before.getValue()).get("achievementName").asText()).isEqualTo("수정 전");
        assertThat(json.readTree(next.getValue()).get("achievementName").asText()).isEqualTo("취업률 실적");
        assertThat(json.readTree(next.getValue()).get("evaluationYear").asText()).isEqualTo("2025");
    }

    @Test
    void scopeUnionAndFilterReachBothListAndCountWithoutR07Expansion() throws Exception {
        CurrentUser multiple = user("R01", "R02", "R04", "R07");
        when(mapper.list(anyMap(), eq(7L), anyList())).thenReturn(List.of(row("DRAFT")));
        when(mapper.count(anyMap(), eq(7L), anyList())).thenReturn(1L);
        mvc.perform(get(BASE).param("managementNo", "ER-51").param("pageSize", "50")
                        .requestAttr("currentUser", multiple))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        ArgumentCaptor<Map<String, Object>> listQuery = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> countQuery = ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(listQuery.capture(), eq(7L), eq(List.of("R01", "R02", "R04")));
        verify(mapper).count(countQuery.capture(), eq(7L), eq(List.of("R01", "R02", "R04")));
        assertThat(listQuery.getValue()).isEqualTo(countQuery.getValue())
                .containsEntry("managementNo", "ER-51").containsEntry("pageSize", 50);
    }

    @Test
    void allNonDraftStatusesRefuseUpdateAndPreserveOriginal() throws Exception {
        for (String state : List.of("SUBMITTED", "DEPARTMENT_CONFIRMED", "DEPARTMENT_REJECTED",
                "CERTIFIED", "CERTIFICATION_REJECTED", "EVALUATION_CONFIRMED")) {
            Map<String, Object> original = row(state);
            when(mapper.find(51L, true)).thenReturn(original);
            mvc.perform(put(BASE + "/51").requestAttr("currentUser", teacher)
                            .contentType(MediaType.APPLICATION_JSON).content(INPUT))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
            assertThat(original.get("certificationStatus")).isEqualTo(state);
        }
        verify(mapper, never()).update(anyMap());
        verify(mapper, never()).history(anyLong(), anyString(), any(), any(), anyLong(), anyString());
    }

    @Test
    void missingOrForeignUpdateAndDetailAreRejected() throws Exception {
        // MyBatis selectOne returns null for no row; Mockito defaults Map returns to an empty map.
        when(mapper.find(999L, true)).thenReturn(null);
        mvc.perform(put(BASE + "/999").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isNotFound());
        Map<String, Object> foreign = row("DRAFT");
        foreign.put("teacherUserId", 8L);
        when(mapper.find(52L, true)).thenReturn(foreign);
        when(mapper.find(52L, false)).thenReturn(foreign);
        mvc.perform(put(BASE + "/52").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/52").requestAttr("currentUser", teacher)).andExpect(status().isForbidden());
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void validationAuthenticationRolesAndFunctionDenyNeverWrite() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        for (String role : List.of("R02", "R04", "R07", "R09")) {
            mvc.perform(post(BASE).requestAttr("currentUser", user(role))
                            .contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isForbidden());
        }
        mvc.perform(post(BASE).requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementDate\":\"2025-04-12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
        mvc.perform(put(BASE + "/51").requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content(INPUT.replace("2025-04-12", "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("achievementDate"));
        mvc.perform(post(BASE).requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON)
                        .content(INPUT.replace("\"attachmentIds\":[]", "\"status\":\"EVALUATION_CONFIRMED\"")))
                .andExpect(status().isBadRequest());
        when(permissions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(post(BASE).requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void configuredDateValueValidationReturnsFieldErrorNotInternalFailure() throws Exception {
        when(mapper.itemRules("FR-032", "2025")).thenReturn(List.of(
                Map.of("requiredYn", "Y", "editableYn", "Y", "dataType", "DATE")));
        mvc.perform(post(BASE).requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("achievementName"));
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void finalizationOrPeriodConflictNeverCreatesOrChangesHistory() throws Exception {
        when(guards.validateMutation(any(), any())).thenThrow(new ConflictException("PERIOD_NOT_ACTIVE"));
        mvc.perform(post(BASE).requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT))
                .andExpect(status().isConflict());
        when(mapper.find(51L, true)).thenReturn(row("DRAFT"));
        doThrow(new ConflictException("CONFIRMED_DATA_LOCKED"))
                .when(guards).validateMutation(any(), any());
        mvc.perform(put(BASE + "/51").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(INPUT)).andExpect(status().isConflict());
        verify(mapper, never()).insert(anyMap());
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void downloadIsReadableXlsxWithScopedDatabaseValues() throws Exception {
        when(mapper.list(anyMap(), eq(7L), eq(List.of("R07")))).thenReturn(List.of(row("DRAFT")));
        byte[] bytes = mvc.perform(get(BASE + "/download").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbook.MIME))
                .andReturn().getResponse().getContentAsByteArray();
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(book.getSheetAt(0).getRow(1).getCell(4).getStringCellValue()).isEqualTo("취업률 실적");
        }
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void bulkRejectsUnapprovedExecutionAndValidatesRoleAndYear() throws Exception {
        String body = "{\"evaluationYear\":\"2025\",\"actionType\":\"GENERATE\",\"targetCondition\":{}}";
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("POLICY_NOT_APPROVED")));
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/bulk-jobs").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionType\":\"DELETE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("evaluationYear"));
        verifyNoInteractions(mapper);
    }

    @Test
    void previewAndOwnerResultAreReadOnlyAndMissingJobIs404() throws Exception {
        when(mapper.job("missing", 7L)).thenReturn(null);
        when(mapper.count(anyMap(), eq(7L), eq(List.of("R07")))).thenReturn(2L);
        mvc.perform(post(BASE + "/bulk-jobs/preview").requestAttr("currentUser", user("R07"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2025\",\"actionType\":\"DELETE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.existingAchievementCount").value(2))
                .andExpect(jsonPath("$.data.policyApproved").value(false));
        when(mapper.job("owned", 7L)).thenReturn(Map.of("jobId", "owned", "totalCount", 2,
                "processedCount", 0, "unprocessedCount", 2));
        when(mapper.jobItems("owned", 7L)).thenReturn(List.of(
                Map.of("targetUserId", 8L, "processedYn", "N", "unprocessedReason", "정책 미승인")));
        mvc.perform(get(BASE + "/bulk-jobs/owned").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].processedYn").value("N"));
        mvc.perform(get(BASE + "/bulk-jobs/missing").requestAttr("currentUser", user("R07")))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/bulk-jobs/owned").requestAttr("currentUser", teacher))
                .andExpect(status().isForbidden());
    }

    private static CurrentUser user(String... roles) {
        return new CurrentUser(7L, "teacher", "E7", "교원", List.of(roles), List.of());
    }

    private static Map<String, Object> row(String status) {
        Map<String, Object> value = new HashMap<>();
        value.put("achievementId", 51L);
        value.put("managementNo", "ER-51");
        value.put("teacherUserId", 7L);
        value.put("teacherName", "교원");
        value.put("evaluationYear", "2025");
        value.put("managementItemCode", "FR-032");
        value.put("achievementDate", LocalDate.of(2025, 4, 12));
        value.put("achievementName", "취업률 실적");
        value.put("attachmentIds", "[]");
        value.put("achievementDetail", "{}");
        value.put("certificationStatus", status);
        return value;
    }
}
