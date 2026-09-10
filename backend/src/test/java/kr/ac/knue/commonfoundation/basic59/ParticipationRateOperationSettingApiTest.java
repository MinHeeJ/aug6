package kr.ac.knue.commonfoundation.basic59;

import static org.hamcrest.Matchers.hasItem;
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
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
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

@WebMvcTest(ParticipationRateOperationSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ParticipationRateOperationSettingApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ParticipationRateOperationSettingService service;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "professor1", "E0001", "교원", List.of("R01"), List.of());
    private final CurrentUser r08 = new CurrentUser(8L, "auditor", "E0008", "감사담당자", List.of("R08"), List.of());

    @Test
    void listParticipationRateOperationSettingsSupportsAreaCategoryFiltersAndPagination() throws Exception {
        when(service.list(any(ParticipationRateOperationSettingSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/business/participation-rate-operation-settings")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR019-LIST")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("achievementAreaCode", "RESEARCH")
                        .param("achievementCategoryCode", "PAPER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participationRateOperationSettings[0].settingId").value(5900201))
                .andExpect(jsonPath("$.data.participationRateOperationSettings[0].achievementAreaCode").value("RESEARCH"))
                .andExpect(jsonPath("$.data.participationRateOperationSettings[0].achievementCategoryCode").value("PAPER"))
                .andExpect(jsonPath("$.data.participationRateOperationSettings[0].participationTypeCode").value("CORRESPONDING_AUTHOR"))
                .andExpect(jsonPath("$.data.participationRateOperationSettings[0].distributionRate").value(70.00))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR019-LIST"));

        mockMvc.perform(get("/api/business/participation-rate-operation-settings")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void saveParticipationRateOperationSettingPersistsMatrixAndReturnsUpdatedRows() throws Exception {
        when(service.save(any(SaveParticipationRateOperationSettingRequest.class), eq(r04), eq("REQ-B59-FR019-SAVE")))
                .thenReturn(new ParticipationRateOperationSettingSaveResponse(List.of(savedRow())));

        mockMvc.perform(post("/api/business/participation-rate-operation-settings/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR019-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(85.50, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participationRateOperationSettings[0].distributionRate").value(85.50))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR019-SAVE"));
    }

    @Test
    void saveParticipationRateOperationSettingReturnsFieldErrorForMissingRates() throws Exception {
        mockMvc.perform(post("/api/business/participation-rate-operation-settings/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleVersionId\":1,\"achievementAreaCode\":\"RESEARCH\",\"achievementCategoryCode\":\"PAPER\",\"managementItemCode\":\"JOURNAL_ARTICLE\",\"changeReason\":\"검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("rates")));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void saveParticipationRateOperationSettingRejectsConfirmedRuleVersionAsConflictNoChange() throws Exception {
        when(service.save(any(SaveParticipationRateOperationSettingRequest.class), eq(r04), any()))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/business/participation-rate-operation-settings/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(45.00, 2L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));
    }

    @Test
    void r01AndR08CannotAccessParticipationRateOperationSettings() throws Exception {
        mockMvc.perform(get("/api/business/participation-rate-operation-settings").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/business/participation-rate-operation-settings/save")
                        .requestAttr("currentUser", r08)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(85.50, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionBeforePersistenceAndRecordsChangeHistoryOnSave() {
        ParticipationRateOperationSettingMapper mapper = org.mockito.Mockito.mock(ParticipationRateOperationSettingMapper.class);
        ParticipationRateOperationSettingService participationService = new ParticipationRateOperationSettingService(mapper);
        when(mapper.findRuleVersionStatus(2L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> participationService.save(validRequest(2L), r04, "REQ-B59-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_RULE_LOCKED");
        verify(mapper, never()).upsertParticipationRateOperationSetting(any(), any(), any(), any());

        when(mapper.findRuleVersionStatus(1L)).thenReturn("DRAFT");
        when(mapper.findRuleVersionEvaluationYear(1L)).thenReturn("2026");
        when(mapper.findByBusinessKey(1L, "2026", "RESEARCH", "PAPER", "JOURNAL_ARTICLE", "TWO", "CORRESPONDING_AUTHOR")).thenReturn(row());
        when(mapper.upsertParticipationRateOperationSetting(any(), any(), eq(4L), eq("REQ-B59-AUDIT"))).thenReturn(savedRow());

        participationService.save(validRequest(1L), r04, "REQ-B59-AUDIT");

        verify(mapper).insertChangeHistory(eq("participation_rate_operation_settings"),
                eq("1:2026:RESEARCH:PAPER:JOURNAL_ARTICLE:TWO:CORRESPONDING_AUTHOR"), eq("UPDATE"),
                eq("distributionRate"), any(), any(), eq(4L), eq("FR-019 저장"), eq("REQ-B59-AUDIT"));
    }

    private ParticipationRateOperationSettingSearchResponse response() {
        return new ParticipationRateOperationSettingSearchResponse(List.of(row()), 0, 20, 1);
    }

    private ParticipationRateOperationSettingRow row() {
        return new ParticipationRateOperationSettingRow(5900201L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "RESEARCH", "PAPER", "JOURNAL_ARTICLE", "TWO", "CORRESPONDING_AUTHOR", new BigDecimal("70.00"), "Y", "B59-SEED-002 논문 공동저자 2인 배분율", 9L, LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private ParticipationRateOperationSettingRow savedRow() {
        return new ParticipationRateOperationSettingRow(5900201L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "RESEARCH", "PAPER", "JOURNAL_ARTICLE", "TWO", "CORRESPONDING_AUTHOR", new BigDecimal("85.50"), "Y", "FR-019 저장", 4L, LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private SaveParticipationRateOperationSettingRequest validRequest(Long ruleVersionId) {
        return new SaveParticipationRateOperationSettingRequest(ruleVersionId, "RESEARCH", "PAPER", "JOURNAL_ARTICLE",
                List.of(new SaveParticipationRateOperationSettingRequest.Rate("TWO", "CORRESPONDING_AUTHOR", new BigDecimal("85.50"))), "FR-019 저장");
    }

    private String validJson(double rate, Long ruleVersionId) {
        return "{\"ruleVersionId\":" + ruleVersionId + ",\"achievementAreaCode\":\"RESEARCH\",\"achievementCategoryCode\":\"PAPER\",\"managementItemCode\":\"JOURNAL_ARTICLE\",\"rates\":[{\"researcherCountBand\":\"TWO\",\"participationTypeCode\":\"CORRESPONDING_AUTHOR\",\"distributionRate\":" + rate + "}],\"changeReason\":\"FR-019 저장\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
