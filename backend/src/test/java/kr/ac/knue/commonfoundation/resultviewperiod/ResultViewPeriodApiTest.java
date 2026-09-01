package kr.ac.knue.commonfoundation.resultviewperiod;

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

@WebMvcTest(ResultViewPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResultViewPeriodApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ResultViewPeriodService service;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void listResultViewPeriodsReturnsPaginationFiltersVisibilityScopeAndRequestIdForR04R09() throws Exception {
        when(service.list(eq(r04), any(ResultViewPeriodSearchCriteria.class))).thenReturn(response());
        when(service.list(eq(r09), any(ResultViewPeriodSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/admin/result-view-periods")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B40-RV-LIST")
                        .param("page", "0")
                        .param("size", "20")
                        .param("evaluationYear", "2026")
                        .param("collegeOrganizationCode", "KNUE-COL-EDU")
                        .param("departmentOrganizationCode", "KNUE-DEPT-COMP")
                        .param("visibilityScope", "SELF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resultViewPeriods[0].settingId").value(501))
                .andExpect(jsonPath("$.data.resultViewPeriods[0].visibilityScope").value("SELF"))
                .andExpect(jsonPath("$.data.resultViewPeriods[0].viewStartAt").value("2026-07-01T09:00:00"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B40-RV-LIST"));

        mockMvc.perform(get("/api/admin/result-view-periods").requestAttr("currentUser", r09).cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void r01CannotListResultViewPeriods() throws Exception {
        mockMvc.perform(get("/api/admin/result-view-periods").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listResultViewPeriodsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/admin/result-view-periods")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("size")));
        verify(service, never()).list(any(), any());
    }

    @Test
    void saveResultViewPeriodPersistsVisibilityScopeAndReturnsRequestIdForR09() throws Exception {
        when(service.save(any(SaveResultViewPeriodRequest.class), eq(r09), eq("REQ-B40-RV-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/result-view-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B40-RV-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(501))
                .andExpect(jsonPath("$.data.visibilityScope").value("SELF"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B40-RV-SAVE"));
    }

    @Test
    void saveResultViewPeriodReturnsFieldErrorsForRequiredVisibilityScope() throws Exception {
        mockMvc.perform(post("/api/admin/result-view-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationYear")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("collegeOrganizationCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("viewStartAt")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("viewEndAt")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("visibilityScope")));
    }

    @Test
    void r01CannotSaveResultViewPeriod() throws Exception {
        mockMvc.perform(post("/api/admin/result-view-periods/save")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsMissingVisibilityScopeAndReversePeriodWithoutPersistence() {
        ResultViewPeriodMapper mapper = org.mockito.Mockito.mock(ResultViewPeriodMapper.class);
        ResultViewPeriodService resultViewPeriodService = new ResultViewPeriodService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resultViewPeriodService.save(new SaveResultViewPeriodRequest(null,
                        "2027", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                        LocalDateTime.parse("2027-07-10T18:00:00"), LocalDateTime.parse("2027-07-01T09:00:00"),
                        "", "Y", "역전 검증"), r09, "REQ-B40-RV-REVERSE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class)
                .hasMessageContaining("결과조회기간");
        verify(mapper, never()).insertResultViewPeriod(any(), any());
        verify(mapper, never()).updateResultViewPeriod(any(), any());
    }

    @Test
    void serviceRejectsActiveOverlapWithoutPersistence() {
        ResultViewPeriodMapper mapper = org.mockito.Mockito.mock(ResultViewPeriodMapper.class);
        ResultViewPeriodService resultViewPeriodService = new ResultViewPeriodService(mapper);
        when(mapper.countOverlappingResultViewPeriods(null, "2027", "KNUE-COL-EDU", "KNUE-DEPT-COMP", "SELF",
                LocalDateTime.parse("2027-07-01T09:00:00"), LocalDateTime.parse("2027-07-10T18:00:00"))).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resultViewPeriodService.save(validRequest(), r09, "REQ-B40-RV-OVERLAP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("중복");
        verify(mapper, never()).insertResultViewPeriod(any(), any());
        verify(mapper, never()).updateResultViewPeriod(any(), any());
    }

    @Test
    void serviceRecordsCreateSideEffectWithRequestId() {
        ResultViewPeriodMapper mapper = org.mockito.Mockito.mock(ResultViewPeriodMapper.class);
        ResultViewPeriodService resultViewPeriodService = new ResultViewPeriodService(mapper);
        when(mapper.countOverlappingResultViewPeriods(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(mapper.insertResultViewPeriod(any(), eq(9L))).thenReturn(row());

        resultViewPeriodService.save(validRequest(), r09, "REQ-B40-RV-AUDIT");

        verify(mapper).insertChangeHistory(eq("result_view_period_settings"), eq("501"), eq("CREATE"), eq("setting"), eq(null),
                eq("2026:KNUE-COL-EDU:KNUE-DEPT-COMP:SELF"), eq(9L), eq("BASIC-40 결과조회기간 저장"), eq("REQ-B40-RV-AUDIT"));
    }

    @Test
    void serviceEvaluatesResultViewAccessWithoutScoreMutationOrConfirmationCancellation() {
        ResultViewPeriodMapper mapper = org.mockito.Mockito.mock(ResultViewPeriodMapper.class);
        ResultViewPeriodService resultViewPeriodService = new ResultViewPeriodService(mapper);
        when(mapper.findActiveResultViewPeriodForAccess("KNUE-DEPT-COMP", "SELF", LocalDateTime.parse("2026-07-01T09:00:00"))).thenReturn(row());
        when(mapper.findActiveResultViewPeriodForAccess("KNUE-DEPT-COMP", "ALL", LocalDateTime.parse("2026-07-11T09:00:00"))).thenReturn(null);

        org.assertj.core.api.Assertions.assertThat(resultViewPeriodService.evaluateAccess("KNUE-DEPT-COMP", "SELF", LocalDateTime.parse("2026-07-01T09:00:00")).allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(resultViewPeriodService.evaluateAccess("KNUE-DEPT-COMP", "ALL", LocalDateTime.parse("2026-07-11T09:00:00")).allowed()).isFalse();
        verify(mapper, never()).insertResultViewPeriod(any(), any());
        verify(mapper, never()).updateResultViewPeriod(any(), any());
    }

    private ResultViewPeriodSearchResponse response() {
        return new ResultViewPeriodSearchResponse(List.of(row()), 0, 20, 1);
    }

    private ResultViewPeriodRow row() {
        return new ResultViewPeriodRow(501L, "2026", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                LocalDateTime.parse("2026-07-01T09:00:00"), LocalDateTime.parse("2026-07-10T18:00:00"),
                "SELF", "Y", 1L, 9L,
                LocalDateTime.parse("2026-08-31T09:00:00"), LocalDateTime.parse("2026-09-01T09:00:00"),
                "BASIC-40 결과조회기간 저장");
    }

    private SaveResultViewPeriodRequest validRequest() {
        return new SaveResultViewPeriodRequest(null, "2027", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                LocalDateTime.parse("2027-07-01T09:00:00"), LocalDateTime.parse("2027-07-10T18:00:00"),
                "SELF", "Y", "BASIC-40 결과조회기간 저장");
    }

    private String validJson() {
        return "{\"evaluationYear\":\"2027\",\"collegeOrganizationCode\":\"KNUE-COL-EDU\",\"departmentOrganizationCode\":\"KNUE-DEPT-COMP\",\"viewStartAt\":\"2027-07-01T09:00:00\",\"viewEndAt\":\"2027-07-10T18:00:00\",\"visibilityScope\":\"SELF\",\"activeYn\":\"Y\",\"changeReason\":\"2027 결과조회기간 등록\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
