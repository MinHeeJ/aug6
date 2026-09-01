package kr.ac.knue.commonfoundation.exceptionperiod;

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

@WebMvcTest(ExceptionPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ExceptionPeriodApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ExceptionPeriodService service;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void listExceptionPeriodsReturnsTargetFiltersApprovalReasonAndRequestIdForR04R09() throws Exception {
        when(service.list(eq(r04), any(ExceptionPeriodSearchCriteria.class))).thenReturn(response());
        when(service.list(eq(r09), any(ExceptionPeriodSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/admin/exception-periods")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B40-EX-LIST")
                        .param("page", "0")
                        .param("size", "20")
                        .param("evaluationYear", "2026")
                        .param("teacherUserId", "2")
                        .param("areaCode", "EDUCATION")
                        .param("targetFunctionCode", "MODIFY_ACHIEVEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exceptionPeriods[0].settingId").value(701))
                .andExpect(jsonPath("$.data.exceptionPeriods[0].teacherUserId").value(2))
                .andExpect(jsonPath("$.data.exceptionPeriods[0].areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.exceptionPeriods[0].targetFunctionCode").value("MODIFY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.exceptionPeriods[0].approvalReason").value("학회 출장으로 승인된 예외"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B40-EX-LIST"));

        mockMvc.perform(get("/api/admin/exception-periods").requestAttr("currentUser", r09).cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void r01CannotListExceptionPeriods() throws Exception {
        mockMvc.perform(get("/api/admin/exception-periods").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listExceptionPeriodsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/admin/exception-periods")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("size")));
        verify(service, never()).list(any(), any());
    }

    @Test
    void saveExceptionPeriodPersistsApprovalReasonAndReturnsRequestIdForR09() throws Exception {
        when(service.save(any(SaveExceptionPeriodRequest.class), eq(r09), eq("REQ-B40-EX-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/exception-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B40-EX-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(701))
                .andExpect(jsonPath("$.data.approvalReason").value("학회 출장으로 승인된 예외"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B40-EX-SAVE"));
    }

    @Test
    void saveExceptionPeriodReturnsFieldErrorsForMissingApprovalReason() throws Exception {
        mockMvc.perform(post("/api/admin/exception-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationYear")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("teacherUserId")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("areaCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("targetFunctionCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("exceptionStartAt")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("exceptionEndAt")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("approvalReason")));
    }

    @Test
    void r01CannotSaveExceptionPeriod() throws Exception {
        mockMvc.perform(post("/api/admin/exception-periods/save")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsMissingApprovalReasonReversePeriodAndInvalidTargetWithoutPersistence() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService exceptionPeriodService = new ExceptionPeriodService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> exceptionPeriodService.save(new SaveExceptionPeriodRequest(null,
                        "2027", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT",
                        LocalDateTime.parse("2027-08-10T18:00:00"), LocalDateTime.parse("2027-08-01T09:00:00"),
                        "", "Y", "역전 검증"), r09, "REQ-B40-EX-REVERSE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class)
                .hasMessageContaining("예외기간");
        verify(mapper, never()).insertExceptionPeriod(any(), any());
        verify(mapper, never()).updateExceptionPeriod(any(), any());
    }

    @Test
    void serviceRejectsActiveOverlapWithoutPersistence() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService exceptionPeriodService = new ExceptionPeriodService(mapper);
        when(mapper.existsTeacherUser(2L)).thenReturn(1);
        when(mapper.existsEvaluationArea("EDUCATION")).thenReturn(1);
        when(mapper.countOverlappingExceptionPeriods(null, "2027", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT",
                LocalDateTime.parse("2027-08-01T09:00:00"), LocalDateTime.parse("2027-08-05T18:00:00"))).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> exceptionPeriodService.save(validRequest(), r09, "REQ-B40-EX-OVERLAP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("중복");
        verify(mapper, never()).insertExceptionPeriod(any(), any());
        verify(mapper, never()).updateExceptionPeriod(any(), any());
    }

    @Test
    void serviceRecordsCreateSideEffectWithRequestIdAndApprovalReason() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService exceptionPeriodService = new ExceptionPeriodService(mapper);
        when(mapper.existsTeacherUser(2L)).thenReturn(1);
        when(mapper.existsEvaluationArea("EDUCATION")).thenReturn(1);
        when(mapper.countOverlappingExceptionPeriods(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(mapper.insertExceptionPeriod(any(), eq(9L))).thenReturn(row());

        exceptionPeriodService.save(validRequest(), r09, "REQ-B40-EX-AUDIT");

        verify(mapper).insertChangeHistory(eq("exception_period_settings"), eq("701"), eq("CREATE"), eq("setting"), eq(null),
                eq("2026:2:EDUCATION:MODIFY_ACHIEVEMENT"), eq(9L), eq("BASIC-40 예외기간 저장"), eq("REQ-B40-EX-AUDIT"));
    }

    @Test
    void exceptionPeriodOverridesGeneralModificationPeriodOnlyForMatchingTargetThenReturnsToGeneralRule() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService exceptionPeriodService = new ExceptionPeriodService(mapper);
        when(mapper.findActiveExceptionPeriodForModification(2L, "EDUCATION", "MODIFY_ACHIEVEMENT", LocalDateTime.parse("2026-08-02T09:00:00"))).thenReturn(row());
        when(mapper.findActiveExceptionPeriodForModification(2L, "EDUCATION", "DELETE_ACHIEVEMENT", LocalDateTime.parse("2026-08-02T09:00:00"))).thenReturn(null);
        when(mapper.countActiveModificationPeriods("2026", "EDUCATION", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-06T09:00:00"))).thenReturn(0);

        org.assertj.core.api.Assertions.assertThat(exceptionPeriodService.evaluateModificationAccess("2026", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-02T09:00:00"), "인증").allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThat(exceptionPeriodService.evaluateModificationAccess("2026", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-02T09:00:00"), "인증").exceptionApplied()).isTrue();
        org.assertj.core.api.Assertions.assertThat(exceptionPeriodService.evaluateModificationAccess("2026", 2L, "EDUCATION", "DELETE_ACHIEVEMENT", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-02T09:00:00"), "인증").allowed()).isFalse();
        org.assertj.core.api.Assertions.assertThat(exceptionPeriodService.evaluateModificationAccess("2026", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-06T09:00:00"), "인증").allowed()).isFalse();
    }

    @Test
    void finalConfirmedStatusBlocksModificationEvenInsideExceptionPeriod() {
        ExceptionPeriodMapper mapper = org.mockito.Mockito.mock(ExceptionPeriodMapper.class);
        ExceptionPeriodService exceptionPeriodService = new ExceptionPeriodService(mapper);

        ExceptionPeriodDecision decision = exceptionPeriodService.evaluateModificationAccess("2026", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT", "KNUE-COL-EDU", LocalDateTime.parse("2026-08-02T09:00:00"), "평가확정");

        org.assertj.core.api.Assertions.assertThat(decision.allowed()).isFalse();
        org.assertj.core.api.Assertions.assertThat(decision.reason()).contains("평가확정");
        verify(mapper, never()).findActiveExceptionPeriodForModification(any(), any(), any(), any());
    }

    private ExceptionPeriodSearchResponse response() {
        return new ExceptionPeriodSearchResponse(List.of(row()), 0, 20, 1);
    }

    private ExceptionPeriodRow row() {
        return new ExceptionPeriodRow(701L, "2026", 2L, "홍길동", "EDUCATION", "MODIFY_ACHIEVEMENT",
                LocalDateTime.parse("2026-08-01T09:00:00"), LocalDateTime.parse("2026-08-05T18:00:00"),
                "학회 출장으로 승인된 예외", "Y", 1L, 9L,
                LocalDateTime.parse("2026-08-31T09:00:00"), LocalDateTime.parse("2026-09-01T09:00:00"),
                "BASIC-40 예외기간 저장");
    }

    private SaveExceptionPeriodRequest validRequest() {
        return new SaveExceptionPeriodRequest(null, "2027", 2L, "EDUCATION", "MODIFY_ACHIEVEMENT",
                LocalDateTime.parse("2027-08-01T09:00:00"), LocalDateTime.parse("2027-08-05T18:00:00"),
                "학회 출장으로 승인된 예외", "Y", "BASIC-40 예외기간 저장");
    }

    private String validJson() {
        return "{\"evaluationYear\":\"2027\",\"teacherUserId\":2,\"areaCode\":\"EDUCATION\",\"targetFunctionCode\":\"MODIFY_ACHIEVEMENT\",\"exceptionStartAt\":\"2027-08-01T09:00:00\",\"exceptionEndAt\":\"2027-08-05T18:00:00\",\"approvalReason\":\"학회 출장으로 승인된 예외\",\"activeYn\":\"Y\",\"changeReason\":\"2027 예외기간 등록\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
