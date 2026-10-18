package kr.ac.knue.commonfoundation.basic69;

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
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingResponses;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingRow;
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

/** Contract tests for the phase-three participation allocation-rate settings API. */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ParticipationAllocationRateSettingsApiTest {
    private static final String LIST_PATH = "/api/admin/participation-allocation-rate-settings";
    private static final String SAVE_PATH = LIST_PATH + "/save";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Basic60Service service;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void getReloadsThePersistedResearcherCountByParticipationTypeMatrix() throws Exception {
        when(service.listParticipationSettings(any())).thenReturn(
                new OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse(
                        List.of(savedRow(2001L, 1, "SOLE", "1.0000"), savedRow(2002L, 2, "LEAD", "0.7000")),
                        0,
                        20,
                        2));

        mockMvc.perform(get(LIST_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-RATE-MATRIX-GET")
                        .param("ruleVersionId", "10")
                        .param("managementItemCode", "B60_SEED_RATE_MATRIX")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].researcherCount").value(1))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].participationType").value("SOLE"))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].allocationRate").value(1.0))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].researcherCount").value(2))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[1].allocationRate").value(0.7))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-RATE-MATRIX-GET"));
    }

    @Test
    void postSavesAMatrixCellAndReturnsThePersistedAllocationRate() throws Exception {
        when(service.saveParticipationSetting(any(), eq(4L), eq("REQ-B69-RATE-SAVE")))
                .thenReturn(savedRow(2002L, 2, "LEAD", "0.7000"));

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-RATE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(2002))
                .andExpect(jsonPath("$.data.researcherCount").value(2))
                .andExpect(jsonPath("$.data.participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.allocationRate").value(0.7))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B69-RATE-SAVE"));
    }

    @Test
    void postRejectsAUserWithoutR04OrR09BeforeChangingTheMatrix() throws Exception {
        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).saveParticipationSetting(any(), any(), any());
    }

    @Test
    void postReportsRuleVersionIdWhenTheRequiredValueIsMissing() throws Exception {
        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'ruleVersionId')]").isNotEmpty());

        verify(service, never()).saveParticipationSetting(any(), any(), any());
    }

    @Test
    void postReturnsTheConfirmedRuleLockCodeWithoutChangingTheMatrix() throws Exception {
        when(service.saveParticipationSetting(any(), eq(4L), eq("REQ-B69-RATE-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post(SAVE_PATH)
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B69-RATE-CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_RULE_LOCKED"));
    }

    private String validPayload(Long ruleVersionId) {
        String ruleVersionField = ruleVersionId == null ? "" : "\"ruleVersionId\":%d,".formatted(ruleVersionId);
        return """
                {%s"targetScope":"COLLEGE_EDU","areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"B60_SEED_RATE_MATRIX","researcherCount":2,"participationType":"LEAD","allocationRate":0.7000,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"배분율 매트릭스 설정"}
                """.formatted(ruleVersionField);
    }

    private OperationalSettingRow savedRow(Long settingId, int researcherCount, String participationType, String allocationRate) {
        return new OperationalSettingRow(settingId, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "RESEARCH", "PAPER", "2026", "AUTHORSHIP", null, "B60_SEED_RATE_MATRIX", null, null, null, researcherCount, participationType, new BigDecimal(allocationRate), null, null, null, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "배분율 매트릭스 설정", 4L, LocalDateTime.parse("2026-09-21T19:40:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
