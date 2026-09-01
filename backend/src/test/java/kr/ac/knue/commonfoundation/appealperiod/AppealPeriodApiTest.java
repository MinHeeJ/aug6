package kr.ac.knue.commonfoundation.appealperiod;

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

@WebMvcTest(AppealPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AppealPeriodApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AppealPeriodService service;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void listAppealPeriodsReturnsPaginationFiltersAndRequestIdForR04R09() throws Exception {
        when(service.list(eq(r04), any(AppealPeriodSearchCriteria.class))).thenReturn(response());
        when(service.list(eq(r09), any(AppealPeriodSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/admin/appeal-periods")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B40-APPEAL-LIST")
                        .param("page", "0")
                        .param("size", "20")
                        .param("evaluationYear", "2026")
                        .param("collegeOrganizationCode", "KNUE-COL-EDU")
                        .param("departmentOrganizationCode", "KNUE-DEPT-COMP")
                        .param("activeYn", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appealPeriods[0].settingId").value(401))
                .andExpect(jsonPath("$.data.appealPeriods[0].appealStartAt").value("2026-06-01T09:00:00"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B40-APPEAL-LIST"));

        mockMvc.perform(get("/api/admin/appeal-periods").requestAttr("currentUser", r09).cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void r01CannotListAppealPeriods() throws Exception {
        mockMvc.perform(get("/api/admin/appeal-periods").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listAppealPeriodsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/admin/appeal-periods")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("size")));
        verify(service, never()).list(any(), any());
    }

    @Test
    void saveAppealPeriodPersistsAndReturnsRequestIdForR09() throws Exception {
        when(service.save(any(SaveAppealPeriodRequest.class), eq(r09), eq("REQ-B40-APPEAL-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/appeal-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B40-APPEAL-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(401))
                .andExpect(jsonPath("$.data.handlerUserId").value(4))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B40-APPEAL-SAVE"));
    }

    @Test
    void saveAppealPeriodReturnsFieldErrorsForRequiredValues() throws Exception {
        mockMvc.perform(post("/api/admin/appeal-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationYear")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("collegeOrganizationCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("handlerUserId")));
    }

    @Test
    void r01CannotSaveAppealPeriod() throws Exception {
        mockMvc.perform(post("/api/admin/appeal-periods/save")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsReversePeriodWithoutPersistence() {
        AppealPeriodMapper mapper = org.mockito.Mockito.mock(AppealPeriodMapper.class);
        AppealPeriodService appealPeriodService = new AppealPeriodService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> appealPeriodService.save(new SaveAppealPeriodRequest(null,
                        "2027", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                        LocalDateTime.parse("2027-06-10T18:00:00"), LocalDateTime.parse("2027-06-01T09:00:00"),
                        4L, "Y", "역전 검증"), r09, "REQ-B40-APPEAL-REVERSE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class)
                .hasMessageContaining("이의신청기간");
        verify(mapper, never()).insertAppealPeriod(any(), any());
        verify(mapper, never()).updateAppealPeriod(any(), any());
    }

    @Test
    void serviceRejectsActiveOverlapWithoutPersistence() {
        AppealPeriodMapper mapper = org.mockito.Mockito.mock(AppealPeriodMapper.class);
        AppealPeriodService appealPeriodService = new AppealPeriodService(mapper);
        when(mapper.existsHandlerUserForAppealPeriod(4L, "KNUE-COL-EDU", "KNUE-DEPT-COMP")).thenReturn(1);
        when(mapper.countOverlappingAppealPeriods(null, "2027", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                LocalDateTime.parse("2027-06-01T09:00:00"), LocalDateTime.parse("2027-06-10T18:00:00"))).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> appealPeriodService.save(validRequest(), r09, "REQ-B40-APPEAL-OVERLAP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("중복");
        verify(mapper, never()).insertAppealPeriod(any(), any());
        verify(mapper, never()).updateAppealPeriod(any(), any());
    }

    @Test
    void serviceRecordsCreateSideEffectWithRequestId() {
        AppealPeriodMapper mapper = org.mockito.Mockito.mock(AppealPeriodMapper.class);
        AppealPeriodService appealPeriodService = new AppealPeriodService(mapper);
        when(mapper.existsHandlerUserForAppealPeriod(4L, "KNUE-COL-EDU", "KNUE-DEPT-COMP")).thenReturn(1);
        when(mapper.countOverlappingAppealPeriods(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(mapper.insertAppealPeriod(any(), eq(9L))).thenReturn(row());

        appealPeriodService.save(validRequest(), r09, "REQ-B40-APPEAL-AUDIT");

        verify(mapper).insertChangeHistory(eq("appeal_period_settings"), eq("401"), eq("CREATE"), eq("setting"), eq(null),
                eq("2026:KNUE-COL-EDU:KNUE-DEPT-COMP:4"), eq(9L), eq("BASIC-40 이의신청기간 저장"), eq("REQ-B40-APPEAL-AUDIT"));
    }

    @Test
    void serviceEvaluatesSubmissionPeriodWithoutCreatingAppealContentRows() {
        AppealPeriodMapper mapper = org.mockito.Mockito.mock(AppealPeriodMapper.class);
        AppealPeriodService appealPeriodService = new AppealPeriodService(mapper);
        when(mapper.findActiveAppealPeriodForSubmission("KNUE-DEPT-COMP", LocalDateTime.parse("2026-06-01T09:00:00"))).thenReturn(row());
        when(mapper.findActiveAppealPeriodForSubmission("KNUE-DEPT-COMP", LocalDateTime.parse("2026-06-11T09:00:00"))).thenReturn(null);

        org.assertj.core.api.Assertions.assertThat(appealPeriodService.evaluateSubmission("KNUE-DEPT-COMP", LocalDateTime.parse("2026-06-01T09:00:00")).allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(appealPeriodService.evaluateSubmission("KNUE-DEPT-COMP", LocalDateTime.parse("2026-06-11T09:00:00")).allowed()).isFalse();
        verify(mapper, never()).insertAppealPeriod(any(), any());
        verify(mapper, never()).updateAppealPeriod(any(), any());
    }

    private AppealPeriodSearchResponse response() {
        return new AppealPeriodSearchResponse(List.of(row()), 0, 20, 1);
    }

    private AppealPeriodRow row() {
        return new AppealPeriodRow(401L, "2026", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                LocalDateTime.parse("2026-06-01T09:00:00"), LocalDateTime.parse("2026-06-10T18:00:00"),
                4L, "Y", 1L, 9L,
                LocalDateTime.parse("2026-08-31T09:00:00"), LocalDateTime.parse("2026-09-01T09:00:00"),
                "BASIC-40 이의신청기간 저장");
    }

    private SaveAppealPeriodRequest validRequest() {
        return new SaveAppealPeriodRequest(null, "2027", "KNUE-COL-EDU", "KNUE-DEPT-COMP",
                LocalDateTime.parse("2027-06-01T09:00:00"), LocalDateTime.parse("2027-06-10T18:00:00"),
                4L, "Y", "BASIC-40 이의신청기간 저장");
    }

    private String validJson() {
        return "{\"evaluationYear\":\"2027\",\"collegeOrganizationCode\":\"KNUE-COL-EDU\",\"departmentOrganizationCode\":\"KNUE-DEPT-COMP\",\"appealStartAt\":\"2027-06-01T09:00:00\",\"appealEndAt\":\"2027-06-10T18:00:00\",\"handlerUserId\":4,\"activeYn\":\"Y\",\"changeReason\":\"2027 이의신청기간 등록\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
