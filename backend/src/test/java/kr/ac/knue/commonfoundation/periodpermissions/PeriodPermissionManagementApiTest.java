package kr.ac.knue.commonfoundation.periodpermissions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.permissionops.PermissionChangeHistoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PeriodPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PeriodPermissionManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean PeriodPermissionService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());

    @Test
    void listPeriodPermissionsReturnsBeforeActiveAfterEffectiveStatesForReq154Req155Req156() throws Exception {
        when(service.listPeriodPermissions(new PeriodPermissionSearchCriteria(0, 10, "BP-2026-A")))
                .thenReturn(new PeriodPermissionSearchResponse(List.of(
                        row(1L, "BEFORE", false), row(2L, "ACTIVE", true), row(3L, "AFTER", false)), 0, 10, 3));

        mockMvc.perform(get("/api/admin/period-permissions")
                        .param("businessPeriodId", "BP-2026-A")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.links[0].periodState").value("BEFORE"))
                .andExpect(jsonPath("$.data.links[0].effectiveAllowed").value(false))
                .andExpect(jsonPath("$.data.links[1].periodState").value("ACTIVE"))
                .andExpect(jsonPath("$.data.links[1].effectiveAllowed").value(true))
                .andExpect(jsonPath("$.data.links[2].periodState").value("AFTER"))
                .andExpect(jsonPath("$.data.links[2].effectiveAllowed").value(false));
    }

    @Test
    void savePeriodPermissionsPersistsLinkAndReturnsRequeryableRowForReq153() throws Exception {
        when(service.savePeriodPermission(any(PeriodPermissionSaveRequest.class), eq(1L)))
                .thenReturn(row(99L, "ACTIVE", true));

        mockMvc.perform(put("/api/admin/period-permissions-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessPeriodId":"BP-2026-A","functionPermissionId":77,"effectiveStartAt":"2026-08-24T00:00:00","effectiveEndAt":"2026-08-31T23:59:59","changeReason":"평가 기간 연결"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.businessPeriodId").value("BP-2026-A"))
                .andExpect(jsonPath("$.data.functionPermissionId").value(77))
                .andExpect(jsonPath("$.data.periodState").value("ACTIVE"))
                .andExpect(jsonPath("$.data.effectiveAllowed").value(true))
                .andExpect(jsonPath("$.data.changeReason").value("평가 기간 연결"));
    }

    @Test
    void savePeriodPermissionsRequiresPeriodRangeValidationForReq153() throws Exception {
        mockMvc.perform(put("/api/admin/period-permissions-save")
                        .requestAttr("currentUser", admin)
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessPeriodId":"BP-2026-A","functionPermissionId":77,"changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("effectiveStartAt"));
    }

    @Test
    void savePeriodPermissionsRequiresSessionCookieBeforeHistorySideEffectForReq170() throws Exception {
        mockMvc.perform(put("/api/admin/period-permissions-save")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessPeriodId":"BP-2026-A","functionPermissionId":77,"effectiveStartAt":"2026-08-24T00:00:00","effectiveEndAt":"2026-08-31T23:59:59","changeReason":"검증"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).savePeriodPermission(any(), any());
    }

    @Test
    void savePeriodPermissionsRejectsEndedPeriodAtServerProcessingTimeWithoutPersistenceForReq156Req169Req170() {
        PeriodPermissionMapper mapper = mock(PeriodPermissionMapper.class);
        PermissionChangeHistoryMapper historyMapper = mock(PermissionChangeHistoryMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC);
        PeriodPermissionService periodPermissionService = new PeriodPermissionService(mapper, historyMapper, clock);
        when(mapper.existsFunctionPermission(77L)).thenReturn(1);

        assertThatThrownBy(() -> periodPermissionService.savePeriodPermission(new PeriodPermissionSaveRequest(
                "BP-2026-A", 77L, LocalDateTime.parse("2026-08-24T00:00:00"), LocalDateTime.parse("2026-08-31T23:59:59"), "종료 후 저장"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("기간별 권한 저장");
        verify(mapper, never()).upsertPeriodPermission(any(), any(), any(), any(), any(), any(), any());
        verify(historyMapper, never()).insertPermissionChangeHistory(any(), any(), any(), any(), any(), any());
    }

    private PeriodPermissionRow row(Long linkId, String periodState, boolean effectiveAllowed) {
        return new PeriodPermissionRow(linkId, "BP-2026-A", 77L, "SCR-PERIOD-PERMISSION-MGMT", "기간별 권한 관리", "R09", "시스템관리자",
                "UPDATE", "ALLOW", LocalDateTime.parse("2026-08-24T00:00:00"), LocalDateTime.parse("2026-08-31T23:59:59"),
                periodState, effectiveAllowed, "평가 기간 연결", LocalDateTime.parse("2026-08-24T09:00:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
