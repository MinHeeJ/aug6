package kr.ac.knue.commonfoundation.basic60;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP contract tests for the FR-019 participation allocation-rate matrix flow.
 *
 * <p>These tests protect the settings-admin boundary, draft/confirmed locks, and the audit
 * transaction expected when a participation allocation rate changes.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({Basic60Service.class, GlobalExceptionHandler.class})
class ParticipationAllocationRateSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Mapper mapper;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void saveThenListReturnsTheSavedParticipationMatrixCell() throws Exception {
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedParticipationSettingImpact(any())).thenReturn(false);
        when(mapper.findParticipationSettingByKey(any())).thenReturn(null, leadAuthorRow());
        when(mapper.listParticipationSettings(any())).thenReturn(List.of(soleAuthorRow(), leadAuthorRow()));
        when(mapper.countParticipationSettings(any())).thenReturn(2L);

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-PARTICIPATION-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("PAPER_SCORE"))
                .andExpect(jsonPath("$.data.researcherCount").value(3))
                .andExpect(jsonPath("$.data.participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.allocationRate").value(0.7))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-PARTICIPATION-SAVE"));

        mockMvc.perform(get("/api/admin/participation-allocation-rate-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-PARTICIPATION-LIST")
                        .param("ruleVersionId", "10")
                        .param("areaCode", "RESEARCH")
                        .param("itemCode", "PAPER")
                        .param("managementItemCode", "PAPER_SCORE")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].researcherCount").value(1))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].participationType").value("SOLE"))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].allocationRate").value(1.0))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].researcherCount").value(3))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].allocationRate").value(0.7))
                .andExpect(jsonPath("$.data.pageSize").value(50))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-PARTICIPATION-LIST"));

        verify(mapper).upsertParticipationSetting(any(), eq(4L));
        verify(mapper).insertChangeHistory(
                eq("participation_allocation_rate_settings"),
                eq("10:COLLEGE_EDU:PAPER_SCORE:3:LEAD"),
                eq("CREATE"),
                eq("allocation_rate"),
                eq(null),
                eq("0.7000"),
                eq(4L),
                eq("논문 대표저자 배분율 설정"),
                eq("REQ-B69-PARTICIPATION-SAVE"));
    }

    @Test
    void saveRejectsR01WithoutWritingAParticipationRate() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(mapper, never()).upsertParticipationSetting(any(), any());
    }

    @Test
    void saveReturnsFieldLevelValidationErrorWhenRuleVersionIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetScope":"COLLEGE_EDU","areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":3,"participationType":"LEAD","allocationRate":0.7000,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"필수값 검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));

        verify(mapper, never()).upsertParticipationSetting(any(), any());
    }

    @Test
    void saveReturnsConfirmedRuleLockedCodeAndLeavesParticipationMatrixUntouched() throws Exception {
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"));

        verify(mapper, never()).upsertParticipationSetting(any(), any());
    }

    @Test
    void saveReturnsConfirmedDataLockedCodeAndLeavesParticipationMatrixUntouched() throws Exception {
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedParticipationSettingImpact(any())).thenReturn(true);

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));

        verify(mapper, never()).upsertParticipationSetting(any(), any());
    }

    private String validPayload(int ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_EDU","areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":3,"participationType":"LEAD","allocationRate":0.7000,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"논문 대표저자 배분율 설정"}
                """.formatted(ruleVersionId);
    }

    private OperationalSettingRow soleAuthorRow() {
        return participationRow(1002L, 1, "SOLE", new BigDecimal("1.0000"));
    }

    private OperationalSettingRow leadAuthorRow() {
        return participationRow(1003L, 3, "LEAD", new BigDecimal("0.7000"));
    }

    private OperationalSettingRow participationRow(Long settingId, int researcherCount, String participationType, BigDecimal allocationRate) {
        return new OperationalSettingRow(
                settingId, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "RESEARCH", "PAPER", "2026",
                "AUTHORSHIP", null, "PAPER_SCORE", null, null, null, researcherCount, participationType, allocationRate,
                null, null, null, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N",
                "논문 대표저자 배분율 설정", 4L, LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
