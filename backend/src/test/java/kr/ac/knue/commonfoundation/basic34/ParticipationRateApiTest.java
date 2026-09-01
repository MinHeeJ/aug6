package kr.ac.knue.commonfoundation.basic34;

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

@WebMvcTest(ParticipationRateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ParticipationRateApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ParticipationRateService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listParticipationRatesReturnsPaginationAndFiltersForReq1000Req1001() throws Exception {
        when(service.list(new ParticipationRateSearchCriteria(0, 20, 10L, 400L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "EVIDENCE", 3, "LEAD", "Y", "주저자")))
                .thenReturn(new ParticipationRateSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/participation-rates")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-PARTICIPATION-LIST")
                        .param("ruleVersionId", "10")
                        .param("managementItemId", "400")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("evaluationYear", "2026")
                        .param("elementCode", "ATTENDANCE")
                        .param("managementItemCode", "EVIDENCE")
                        .param("researcherCount", "3")
                        .param("participationType", "LEAD")
                        .param("activeYn", "Y")
                        .param("keyword", "주저자"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participationRates[0].participationRateRuleId").value(710))
                .andExpect(jsonPath("$.data.participationRates[0].managementItemId").value(400))
                .andExpect(jsonPath("$.data.participationRates[0].researcherCount").value(3))
                .andExpect(jsonPath("$.data.participationRates[0].participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.participationRates[0].distributionRate").value(0.5))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-PARTICIPATION-LIST"));
    }

    @Test
    void r01CannotListParticipationRatesForReq1000() throws Exception {
        mockMvc.perform(get("/api/admin/participation-rates")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listParticipationRatesRejectsInvalidPageSizeForReq1000() throws Exception {
        mockMvc.perform(get("/api/admin/participation-rates")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
        verify(service, never()).list(any());
    }

    @Test
    void postParticipationRatesPersistsDraftRateRuleForOpenApiContract() throws Exception {
        when(service.save(any(SaveParticipationRateRequest.class), eq(1L), eq("REQ-B34-PARTICIPATION-POST"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/participation-rates")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-PARTICIPATION-POST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"managementItemId":400,"researcherCount":3,"participationType":"LEAD","distributionRate":0.5,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"배분율 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participationRateRuleId").value(710))
                .andExpect(jsonPath("$.data.ruleVersionStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.activeYn").value("Y"));
        verify(service).save(any(SaveParticipationRateRequest.class), eq(1L), eq("REQ-B34-PARTICIPATION-POST"));
    }

    @Test
    void saveParticipationRatePersistsDraftRateAndReturnsRequestIdForReq1001Req1002() throws Exception {
        when(service.save(any(SaveParticipationRateRequest.class), eq(1L), eq("REQ-B34-PARTICIPATION-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/participation-rates/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-PARTICIPATION-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"managementItemId":400,"researcherCount":3,"participationType":"LEAD","distributionRate":0.5,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"배분율 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationRateRuleId").value(710))
                .andExpect(jsonPath("$.data.managementItemCode").value("EVIDENCE"))
                .andExpect(jsonPath("$.data.participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.distributionRate").value(0.5))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-PARTICIPATION-SAVE"));
    }

    @Test
    void saveParticipationRateRequiresRuleVersionIdForReq1001() throws Exception {
        mockMvc.perform(post("/api/admin/participation-rates/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemId":400,"researcherCount":3,"participationType":"LEAD","distributionRate":0.5,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveParticipationRateRejectsConfirmedRuleVersionForReq1007() throws Exception {
        when(service.save(any(SaveParticipationRateRequest.class), eq(1L), eq("REQ-B34-PARTICIPATION-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 배분율은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/participation-rates/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-PARTICIPATION-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"managementItemId":400,"researcherCount":3,"participationType":"LEAD","distributionRate":0.5,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveParticipationRateForReq1001() throws Exception {
        mockMvc.perform(post("/api/admin/participation-rates/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"managementItemId":400,"researcherCount":3,"participationType":"LEAD","distributionRate":0.5,"effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","activeYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingParticipationRateForReq1007() {
        ParticipationRateMapper mapper = org.mockito.Mockito.mock(ParticipationRateMapper.class);
        ParticipationRateService participationRateService = new ParticipationRateService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> participationRateService.save(
                        request(11L, BigDecimal.valueOf(0.5)), 1L, "REQ-B34-PARTICIPATION-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertParticipationRate(any(), any());
    }

    @Test
    void serviceRecordsParticipationRateChangeHistoryWithRequestIdForReq1002() {
        ParticipationRateMapper mapper = org.mockito.Mockito.mock(ParticipationRateMapper.class);
        ParticipationRateService participationRateService = new ParticipationRateService(mapper);
        ParticipationRateRow before = row(BigDecimal.valueOf(0.4));
        ParticipationRateRow after = row(BigDecimal.valueOf(0.5));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.managementItemBelongsToRuleVersion(10L, 400L)).thenReturn(true);
        when(mapper.findByKey(any(SaveParticipationRateRequest.class))).thenReturn(before, after);

        participationRateService.save(request(10L, BigDecimal.valueOf(0.5)), 1L, "REQ-B34-PARTICIPATION-AUDIT");

        verify(mapper).upsertParticipationRate(any(SaveParticipationRateRequest.class), eq(1L));
        verify(mapper).insertChangeHistory(eq("participation_rate_rules"), eq("10:400:3:LEAD:2026-01-01:2026-12-31"), eq("UPDATE"), eq("distribution_rate"), eq("0.4"), eq("0.5"), eq(1L), eq("배분율 정비"), eq("REQ-B34-PARTICIPATION-AUDIT"));
    }

    private SaveParticipationRateRequest request(Long ruleVersionId, BigDecimal distributionRate) {
        return new SaveParticipationRateRequest(ruleVersionId, 400L, 3, "LEAD", distributionRate,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "Y", "배분율 정비");
    }

    private ParticipationRateRow row() {
        return row(BigDecimal.valueOf(0.5));
    }

    private ParticipationRateRow row(BigDecimal distributionRate) {
        return new ParticipationRateRow(710L, 10L, "B34-DRAFT-2026", "DRAFT", 400L, "EDUCATION", "교육", "LECTURE",
                "강의", "2026", "ATTENDANCE", "출석", "EVIDENCE", "증빙파일", 3, "LEAD", "주저자",
                distributionRate, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "Y",
                "배분율 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
