package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Common routes execute the real feature workflow via the shared port, without a database. */
class EmploymentRateCommonExcelApiTest {
    private static final String BUSINESS = "EMPLOYMENT_RATE_ACHIEVEMENT";
    private final ExcelOperationsMapper common = mock(ExcelOperationsMapper.class);
    private final ExcelOperationsService generic = mock(ExcelOperationsService.class);
    private final EmploymentRateExcelMapper domain = mock(EmploymentRateExcelMapper.class);
    private final EmploymentRateAchievementMapper achievements = mock(EmploymentRateAchievementMapper.class);
    private final EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
    private final EmploymentRateAchievementService authorization = mock(EmploymentRateAchievementService.class);
    private final CurrentUser staff = new CurrentUser(3L, "staff", "E3001", "직원", List.of("R07"), List.of());
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ExcelBusinessWorkflow workflow = new EmploymentRateExcelService(
                common, domain, guards, new ObjectMapper(), achievements, authorization);
        mvc = MockMvcBuilders.standaloneSetup(new ExcelOperationsController(generic, List.of(workflow)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(domain.findTemplateId()).thenReturn("ER-TPL");
        when(common.findUploadTemplate("ER-TPL")).thenReturn(new ExcelTemplateRow(
                "ER-TPL", BUSINESS, "v1.0", LocalDate.of(2026, 1, 1), "Y", "ACTIVE", "token", "template.xlsx"));
    }

    @Test
    void r07CanOnlyReadEmploymentTemplateAndReceivesRealXlsx() throws Exception {
        mvc.perform(get("/api/admin/excel-upload-templates").param("businessType", BUSINESS)
                .requestAttr("currentUser", staff)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templates[0].businessType").value(BUSINESS));
        byte[] bytes = mvc.perform(get("/api/admin/excel-upload-templates/ER-TPL/file")
                .requestAttr("currentUser", staff)).andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateWorkbook.read(bytes).get(0).cells()).isEqualTo(EmploymentRateExcelService.HEADERS);
        mvc.perform(get("/api/admin/excel-upload-templates").requestAttr("currentUser", staff))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/excel-upload-templates").param("businessType", "OTHER")
                .requestAttr("currentUser", staff)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/excel-upload-templates/OTHER/file").requestAttr("currentUser", staff))
                .andExpect(status().isForbidden());
        verifyNoInteractions(generic);
    }

    @Test
    void diagnosticsHistoryDownloadAndCommitEnforceUploadOwner() throws Exception {
        when(domain.findUpload("ER-UP-owned", false)).thenReturn(Map.of(
                "uploaderUserId", 9L, "validationStatus", "VALIDATED"));
        when(domain.findUpload("ER-UP-owned", true)).thenReturn(Map.of(
                "uploaderUserId", 9L, "validationStatus", "VALIDATED"));
        for (String route : List.of("/api/admin/excel-upload-errors", "/api/admin/excel-upload-errors/download",
                "/api/admin/excel-upload-histories")) {
            mvc.perform(get(route).param("uploadId", "ER-UP-owned").requestAttr("currentUser", staff))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/admin/excel-uploads/ER-UP-owned/commit").requestAttr("currentUser", staff))
                .andExpect(status().isForbidden());
        verifyNoInteractions(generic, achievements);
        verify(common, never()).listExcelUploadErrors(anyString(), anyInt(), anyInt());
    }

    @Test
    void ownerErrorDownloadIsActualWorkbookAndUnfilteredHistoryRemainsOwnerScoped() throws Exception {
        when(domain.findUpload("ER-UP-owned", false)).thenReturn(Map.of(
                "uploaderUserId", 3L, "validationStatus", "REJECTED"));
        when(common.listExcelUploadErrors("ER-UP-owned", Integer.MAX_VALUE, 0)).thenReturn(List.of(
                new ExcelUploadErrorRow("ERR", "ER-UP-owned", 2, "교번", "E999", "OUT_OF_SCOPE", "범위 오류", "수정")));
        byte[] bytes = mvc.perform(get("/api/admin/excel-upload-errors/download").param("uploadId", "ER-UP-owned")
                .requestAttr("currentUser", staff)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(EmploymentRateWorkbook.read(bytes).get(1).cells()).contains("OUT_OF_SCOPE");
        mvc.perform(get("/api/admin/excel-upload-histories").requestAttr("currentUser", staff))
                .andExpect(status().isOk());
        verify(domain).histories(3L, false, null, null, 20, 0);
        verifyNoInteractions(generic);
    }

    @Test
    void commonUploadReturns200ForRowErrorsBut400ForMalformedAndWrongHeader() throws Exception {
        List<String> refs = List.of("users.employee_no",
                "evaluation_element_management_item_settings.management_item_code",
                "ISO_DATE", "OPTIONAL_TEXT", "OPTIONAL_OWNED_FILE_TOKEN");
        when(common.listTemplateRules("ER-TPL")).thenReturn(java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> new ExcelTemplateRuleRow("RULE-" + index,
                        EmploymentRateExcelService.HEADERS.get(index), index + 1, refs.get(index))).toList());
        byte[] rowError = EmploymentRateWorkbook.write(List.of(EmploymentRateExcelService.HEADERS,
                List.of("UNKNOWN", "EDU001", "2026-04-10", "실적", "")));
        mvc.perform(multipart("/api/admin/excel-uploads")
                .file(new MockMultipartFile("file", "rows.xlsx", "application/octet-stream", rowError))
                .param("businessType", BUSINESS)
                .requestAttr("currentUser", staff)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationStatus").value("ERROR"))
                .andExpect(jsonPath("$.data.savedCount").value(0));
        mvc.perform(multipart("/api/admin/excel-uploads")
                .file(new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[] {1, 2}))
                .param("businessType", BUSINESS)
                .requestAttr("currentUser", staff)).andExpect(status().isBadRequest());
        byte[] wrongHeader = EmploymentRateWorkbook.write(List.of(List.of("잘못된 양식")));
        mvc.perform(multipart("/api/admin/excel-uploads")
                .file(new MockMultipartFile("file", "header.xlsx", "application/octet-stream", wrongHeader))
                .param("businessType", BUSINESS)
                .requestAttr("currentUser", staff)).andExpect(status().isBadRequest());
        verifyNoInteractions(generic, achievements);
    }

    @Test
    void commonCommitMaterializesSourceUsingWorkflowRatherThanGenericBookkeeping() throws Exception {
        when(domain.findUpload("ER-UP-ok", false)).thenReturn(Map.of(
                "uploaderUserId", 3L, "validationStatus", "VALIDATED"));
        when(domain.findUpload("ER-UP-ok", true)).thenReturn(Map.of(
                "uploaderUserId", 3L, "validationStatus", "VALIDATED"));
        when(domain.stagedRows("ER-UP-ok")).thenReturn(List.of(Map.of(
                "rowNumber", 2, "payload", new ObjectMapper().writeValueAsString(Map.of(
                        "rawCells", List.of("E1001", "EDU001", "2026-04-10", "실적", ""))))));
        when(domain.findActiveTeacher("E1001")).thenReturn(2L);
        when(domain.countActiveItems("EDU001")).thenReturn(1);
        when(guards.countCertificationScope(3L, 2L)).thenReturn(1);
        when(guards.countActiveInputPeriods("2026", 2L)).thenReturn(1);
        when(achievements.organizations(eq(2L), any())).thenReturn(List.of("ORG"));
        when(achievements.insert(anyMap())).thenAnswer(call -> {
            Map<String, Object> row = call.getArgument(0);
            row.put("achievementId", 88L);
            return 1;
        });
        when(achievements.history(anyMap())).thenReturn(1);
        mvc.perform(post("/api/admin/excel-uploads/ER-UP-ok/commit").requestAttr("currentUser", staff)
                .header("X-Request-Id", "COMMON-COMMIT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(1));
        verify(achievements).insert(anyMap());
        verify(common).markUploadCommitted("ER-UP-ok");
        verifyNoInteractions(generic);
    }
}
