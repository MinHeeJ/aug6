package kr.ac.knue.commonfoundation.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SensitiveInformationAccessLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SensitiveInformationAccessLogApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean SensitiveInformationAccessLogService service;

    @Test
    void listSensitiveInformationAccessLogsReturnsEnvelopeWithoutProtectedPlaintext() throws Exception {
        SensitiveInformationAccessLogRow row = sensitiveLog("PERSONAL_INFORMATION", "SUCCESS");
        when(service.listSensitiveInformationAccessLogs(eq(0), eq(20), any(SensitiveInformationAccessLogSearchCriteria.class)))
                .thenReturn(new SensitiveInformationAccessLogSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/admin/audit/sensitive-information-access-logs")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .param("filter", "admin")
                        .param("informationType", "PERSONAL_INFORMATION")
                        .param("viewerUserId", "1")
                        .param("accessResult", "SUCCESS")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-31")
                        .param("page", "0")
                        .param("size", "20")
                        .header("X-Request-Id", "REQ-SENSITIVE-QUERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logs[0].accessLogId").value(1))
                .andExpect(jsonPath("$.data.logs[0].informationType").value("PERSONAL_INFORMATION"))
                .andExpect(jsonPath("$.data.logs[0].viewerUserId").value(1))
                .andExpect(jsonPath("$.data.logs[0].viewerLoginId").value("admin"))
                .andExpect(jsonPath("$.data.logs[0].viewerName").value("시스템 관리자"))
                .andExpect(jsonPath("$.data.logs[0].targetScope").value("교직원 3명 개인정보 조회 범위"))
                .andExpect(jsonPath("$.data.logs[0].accessPurpose").value("감사 요청 검토"))
                .andExpect(jsonPath("$.data.logs[0].purposeSource").value("USER_INPUT"))
                .andExpect(jsonPath("$.data.logs[0].accessResult").value("SUCCESS"))
                .andExpect(jsonPath("$.data.logs[0].requestId").value("REQ-SENSITIVE-SEED"))
                .andExpect(jsonPath("$.data.logs[0].protectedPlainValue").doesNotExist())
                .andExpect(jsonPath("$.data.logs[0].accountNumberPlain").doesNotExist())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-SENSITIVE-QUERY"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listSensitiveInformationAccessLogsRejectsNonR09BeforeMapperLookup() throws Exception {
        mockMvc.perform(get("/api/admin/audit/sensitive-information-access-logs")
                        .requestAttr("currentUser", new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of()))
                        .cookie(adminCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).listSensitiveInformationAccessLogs(anyInt(), anyInt(), any());
    }

    @Test
    void serviceRejectsInvalidInformationResultAndDateRangeWithoutMapperLookup() {
        SensitiveInformationAccessLogMapper mapper = mock(SensitiveInformationAccessLogMapper.class);
        SensitiveInformationAccessLogService logService = new SensitiveInformationAccessLogService(mapper);
        SensitiveInformationAccessLogSearchCriteria invalid = new SensitiveInformationAccessLogSearchCriteria(
                "admin", "PLAIN_ACCOUNT_NUMBER", 1L, "BAD_RESULT", LocalDate.parse("2026-08-31"), LocalDate.parse("2026-08-01"));

        assertThatThrownBy(() -> logService.listSensitiveInformationAccessLogs(0, 20, invalid))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("중요정보");
        verify(mapper, never()).listSensitiveInformationAccessLogs(any(), anyInt(), anyInt());
    }

    @Test
    void serviceDefaultsSensitiveInformationAccessLogPaginationToAllowedSizes() {
        SensitiveInformationAccessLogMapper mapper = mock(SensitiveInformationAccessLogMapper.class);
        SensitiveInformationAccessLogService logService = new SensitiveInformationAccessLogService(mapper);
        when(mapper.listSensitiveInformationAccessLogs(any(), eq(20), eq(0))).thenReturn(List.of());
        when(mapper.countSensitiveInformationAccessLogs(any())).thenReturn(0L);

        SensitiveInformationAccessLogSearchResponse response = logService.listSensitiveInformationAccessLogs(-1, 7,
                new SensitiveInformationAccessLogSearchCriteria(null, null, null, null, null, null));

        org.assertj.core.api.Assertions.assertThat(response.page()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.size()).isEqualTo(20);
        verify(mapper).listSensitiveInformationAccessLogs(any(), eq(20), eq(0));
    }

    private SensitiveInformationAccessLogRow sensitiveLog(String informationType, String accessResult) {
        return new SensitiveInformationAccessLogRow(1L, informationType, 1L, "admin", "시스템 관리자",
                "교직원 3명 개인정보 조회 범위", "감사 요청 검토", "USER_INPUT", accessResult,
                "REQ-SENSITIVE-SEED", LocalDateTime.parse("2026-08-27T10:15:00"));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
