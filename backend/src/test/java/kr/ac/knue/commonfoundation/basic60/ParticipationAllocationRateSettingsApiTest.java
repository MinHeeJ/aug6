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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
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
 * HTTP contract tests for the participation-allocation-rate matrix endpoints.
 *
 * <p>The matrix save test verifies the authoritative
 * {@code SaveParticipationAllocationRateSettingsRequest} envelope ({@code ruleVersionId} plus {@code items}) and
 * persists its rows together with their change histories.</p>
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ParticipationAllocationRateSettingsApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    Basic60Service service;

    private final CurrentUser settingsAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void bulkMatrixSaveIsReturnedByTheFollowUpMatrixQuery() throws Exception {
        when(service.listParticipationSettings(any())).thenReturn(
                new OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse(
                        List.of(singleAuthorRow(), jointAuthorRow()), 0, 20, 2));

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-PARTICIPATION-MATRIX-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matrixPayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B60-PARTICIPATION-MATRIX-001"));

        mockMvc.perform(get("/api/admin/participation-allocation-rate-settings")
                        .requestAttr("currentUser", settingsAdmin)
                        .param("ruleVersionId", "10")
                        .param("areaCode", "RESEARCH")
                        .param("itemCode", "PAPER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationAllocationRateSettings.length()").value(2))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].researcherCount").value(1))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].allocationRate").value(1.0))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].researcherCount").value(2))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].allocationRate").value(0.7));
    }

    @Test
    void saveRejectsAUserWithoutTheSettingsRole() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matrixPayload(10)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).saveParticipationSettings(any(), any(), any());
    }

    @Test
    void saveReportsRuleVersionIdWhenItIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matrixPayloadWithoutRuleVersion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());

        verify(service, never()).saveParticipationSettings(any(), any(), any());
    }

    @Test
    void saveReturnsTheSharedConflictEnvelopeForConfirmedRuleLock() throws Exception {
        when(service.saveParticipationSettings(any(), eq(4L), eq("REQ-B60-PARTICIPATION-RULE-LOCK")))
                .thenThrow(new ConflictException(
                        "확정 또는 폐기된 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-PARTICIPATION-RULE-LOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matrixPayload(11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("확정 또는 폐기된 규정버전은 수정할 수 없습니다."));
    }

    @Test
    void successfulSaveCarriesTheHistoryCorrelationRequestIdToTheTransactionBoundary() throws Exception {
        when(service.saveParticipationSettings(any(), eq(4L), eq("REQ-B60-PARTICIPATION-HISTORY-001")))
                .thenReturn(List.of(jointAuthorRow()));

        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", settingsAdmin)
                        .header("X-Request-Id", "REQ-B60-PARTICIPATION-HISTORY-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matrixPayload(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].participationType").value("LEAD"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B60-PARTICIPATION-HISTORY-001"));

        verify(service).saveParticipationSettings(any(), eq(4L), eq("REQ-B60-PARTICIPATION-HISTORY-001"));
    }

    private String matrixPayload(int ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_RESEARCH","changeReason":"논문 저자수별 배분율 일괄 조정","items":[
                  {"areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":1,"participationType":"SOLE","allocationRate":1.00,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31"},
                  {"areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":2,"participationType":"LEAD","allocationRate":0.70,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31"}
                ]}
                """.formatted(ruleVersionId);
    }

    private String matrixPayloadWithoutRuleVersion() {
        return """
                {"targetScope":"COLLEGE_RESEARCH","changeReason":"저자수별 배분율 조정","items":[
                  {"areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":2,"participationType":"LEAD","allocationRate":0.70,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31"}
                ]}
                """;
    }

    private OperationalSettingRow singleAuthorRow() {
        return row(2001L, 1, "SOLE", BigDecimal.ONE);
    }

    private OperationalSettingRow jointAuthorRow() {
        return row(2002L, 2, "LEAD", BigDecimal.valueOf(0.70));
    }

    private OperationalSettingRow row(Long settingId, int researcherCount, String participationType, BigDecimal allocationRate) {
        return new OperationalSettingRow(
                settingId, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_RESEARCH", "RESEARCH", "PAPER",
                "2026", "AUTHORSHIP", null, "PAPER_SCORE", null, null, null, researcherCount,
                participationType, allocationRate, null, null, null, "Y", null,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N",
                "저자수별 배분율 조정", 4L, LocalDateTime.parse("2026-09-21T09:00:00"));
    }
}
