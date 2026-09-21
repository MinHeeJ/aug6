package kr.ac.knue.commonfoundation.basic70;

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
import kr.ac.knue.commonfoundation.basic60.Basic60Controller;
import kr.ac.knue.commonfoundation.basic60.Basic60Mapper;
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingResponses;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingRow;
import kr.ac.knue.commonfoundation.basic60.SaveParticipationAllocationRateSettingRequest;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
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
 * Defines the Phase 3 participation-allocation matrix and batch-save HTTP contract.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ParticipationAllocationRateSettingsApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Service service;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void r04CanReadTheB60SeedMatrixAndSaveItsItemsAsOneBatch() throws Exception {
        OperationalSettingRow soleAuthor = participationRow(2001L, 1, "SOLE", "1.00", "Y");
        OperationalSettingRow twoAuthors = participationRow(2002L, 2, "CO", "0.50", "Y");
        OperationalSettingRow threeOrMoreAuthors = participationRow(2003L, 3, "LEAD", "0.70", "Y");
        when(service.listParticipationSettings(any())).thenReturn(
                new OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse(
                        List.of(soleAuthor, twoAuthors, threeOrMoreAuthors), 0, 20, 3));
        when(service.saveParticipationSetting(any(), eq(4L), eq("REQ-B70-PARTICIPATION-BATCH")))
                .thenReturn(threeOrMoreAuthors);

        mockMvc.perform(get("/api/admin/participation-allocation-rate-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-PARTICIPATION-LIST")
                        .param("targetScope", "COLLEGE_EDU")
                        .param("areaCode", "RESEARCH")
                        .param("managementItemCode", "PAPER_SCORE")
                        .param("activeYn", "Y")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings.length()").value(3))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].researcherCount").value(1))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].researcherCount").value(2))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[2].researcherCount").value(3))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[2].allocationRate").value(0.7))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-PARTICIPATION-LIST"));

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-PARTICIPATION-BATCH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-PARTICIPATION-BATCH"));
    }

    @Test
    void r01CannotSubmitTheParticipationAllocationBatch() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).saveParticipationSetting(any(), any(), any());
    }

    @Test
    void batchWithoutRuleVersionReturnsTheRuleVersionFieldError() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());
        verify(service, never()).saveParticipationSetting(any(), any(), any());
    }

    @Test
    void confirmedRuleVersionBatchReturnsTheConfirmedRuleLock() throws Exception {
        when(service.saveParticipationSetting(any(), eq(4L), eq("REQ-B70-PARTICIPATION-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B70-PARTICIPATION-CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B70-PARTICIPATION-CONFIRMED"));
    }

    @Test
    void serviceWritesParticipationAllocationAuditHistoryWithTheRequestId() {
        Basic60Mapper mapper = org.mockito.Mockito.mock(Basic60Mapper.class);
        Basic60Service basic60Service = new Basic60Service(mapper);
        SaveParticipationAllocationRateSettingRequest request = new SaveParticipationAllocationRateSettingRequest(
                10L, "COLLEGE_EDU", "RESEARCH", "PAPER", "2026", "AUTHORSHIP", "PAPER_SCORE", 3,
                "LEAD", new BigDecimal("0.70"), "Y", LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"), "배분율 변경");
        OperationalSettingRow before = participationRow(2003L, 3, "LEAD", "0.50", "Y");
        OperationalSettingRow after = participationRow(2003L, 3, "LEAD", "0.70", "Y");
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedParticipationSettingImpact(request)).thenReturn(false);
        when(mapper.findParticipationSettingByKey(request)).thenReturn(before, after);

        basic60Service.saveParticipationSetting(request, 4L, "REQ-B70-PARTICIPATION-AUDIT");

        verify(mapper).upsertParticipationSetting(request, 4L);
        verify(mapper).insertChangeHistory(
                "participation_allocation_rate_settings", "10:COLLEGE_EDU:PAPER_SCORE:3:LEAD",
                "UPDATE", "allocation_rate", "0.50", "0.70", 4L, "배분율 변경",
                "REQ-B70-PARTICIPATION-AUDIT");
    }

    private OperationalSettingRow participationRow(
            Long settingId, int researcherCount, String participationType, String allocationRate, String activeYn) {
        return new OperationalSettingRow(
                settingId, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "RESEARCH", "PAPER", "2026",
                "AUTHORSHIP", null, "PAPER_SCORE", null, null, null, researcherCount, participationType,
                new BigDecimal(allocationRate), null, null, null, activeYn, null,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "배분율 설정", 4L,
                LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private String batchPayload() {
        return batchPayload(10L);
    }

    private String batchPayload(Long ruleVersionId) {
        return """
                {%s"targetScope":"COLLEGE_EDU","changeReason":"참여구분별 배분율 일괄 변경","items":[
                  {"areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":1,"participationType":"SOLE","allocationRate":1.00,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31"},
                  {"areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":2,"participationType":"CO","allocationRate":0.50,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31"},
                  {"areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":3,"participationType":"LEAD","allocationRate":0.70,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31"}
                ]}
                """.formatted(ruleVersionId == null ? "" : "\"ruleVersionId\":" + ruleVersionId + ",");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
