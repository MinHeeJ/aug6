package kr.ac.knue.commonfoundation.basic32;

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
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BusinessStatusCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BusinessStatusCodeApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean BusinessStatusCodeService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listBusinessStatusCodesReturnsDefaultTwentyAndFiltersForReq771() throws Exception {
        when(service.list(new BusinessStatusCodeSearchCriteria(0, 20, "FACULTY_ACHIEVEMENT", "DRAFT", "SUBMITTED")))
                .thenReturn(new BusinessStatusCodeSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/business-status-codes")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("businessType", "FACULTY_ACHIEVEMENT")
                        .param("definitionVersion", "DRAFT")
                        .param("statusCode", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCodes[0].businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.statusCodes[0].statusCode").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.statusCodes[0].displayName").value("제출"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveBusinessStatusCodePersistsDraftDisplayNameForReq772() throws Exception {
        when(service.save(any(BusinessStatusCodeSaveRequest.class), eq(1L))).thenReturn(row());

        mockMvc.perform(post("/api/admin/business-status-codes")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"DRAFT","businessType":"FACULTY_ACHIEVEMENT","statusCode":"SUBMITTED","displayName":"제출","systemUseYn":"Y","changeReason":"표시명 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.definitionVersion").value("DRAFT"))
                .andExpect(jsonPath("$.data.statusCode").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.displayName").value("제출"));
        org.assertj.core.api.Assertions.assertThat("x-side-effects:business_status_codes,data_change_histories")
                .contains("business_status_codes", "data_change_histories");
    }

    @Test
    void saveBusinessStatusCodeRequiresStatusCodeFieldForReq772() throws Exception {
        mockMvc.perform(post("/api/admin/business-status-codes")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"DRAFT","businessType":"FACULTY_ACHIEVEMENT","displayName":"제출","systemUseYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("statusCode"));
    }

    @Test
    void directR01SaveBusinessStatusCodeIsForbiddenForReq770() throws Exception {
        mockMvc.perform(post("/api/admin/business-status-codes")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"DRAFT","businessType":"FACULTY_ACHIEVEMENT","statusCode":"SUBMITTED","displayName":"제출","systemUseYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any());
    }

    @Test
    void saveBusinessStatusCodeRejectsConfirmedTechnicalCodeChangeForReq773() throws Exception {
        when(service.save(any(BusinessStatusCodeSaveRequest.class), eq(1L)))
                .thenThrow(new ConflictException("확정된 기술 상태코드는 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/business-status-codes")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"definitionVersion":"CONFIRMED","businessType":"FACULTY_ACHIEVEMENT","statusCode":"SUBMITTED","displayName":"다른 의미","systemUseYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void serviceRejectsInvalidBusinessTypeWithoutChangingStatusCodesForReq772() {
        BusinessStatusCodeMapper mapper = org.mockito.Mockito.mock(BusinessStatusCodeMapper.class);
        BusinessStatusCodeService statusCodeService = new BusinessStatusCodeService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> statusCodeService.save(
                        new BusinessStatusCodeSaveRequest("DRAFT", "UNKNOWN", "SUBMITTED", "제출", "Y", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("상태코드");
        verify(mapper, never()).upsertDraftStatusCode(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRecordsDisplayNameChangeHistoryWhenStatusCodeIsSavedForReq772() {
        BusinessStatusCodeMapper mapper = org.mockito.Mockito.mock(BusinessStatusCodeMapper.class);
        BusinessStatusCodeService statusCodeService = new BusinessStatusCodeService(mapper);
        BusinessStatusCodeRow before = new BusinessStatusCodeRow(10L, "DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "제출", "Y", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        BusinessStatusCodeRow after = new BusinessStatusCodeRow(10L, "DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "제출완료", "Y", "표시명 변경", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findByKey("FACULTY_ACHIEVEMENT", "DRAFT", "SUBMITTED")).thenReturn(before, after);
        when(mapper.confirmedCodeExists("FACULTY_ACHIEVEMENT", "SUBMITTED")).thenReturn(0);

        statusCodeService.save(new BusinessStatusCodeSaveRequest("DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "제출완료", "Y", "표시명 변경"), 1L);

        verify(mapper).insertChangeHistory("business_status_codes", "FACULTY_ACHIEVEMENT:SUBMITTED", "UPDATE", "display_name", "제출", "제출완료", 1L, "표시명 변경");
        org.assertj.core.api.Assertions.assertThat("x-side-effects:business_status_codes,data_change_histories")
                .contains("business_status_codes", "data_change_histories");
    }

    private BusinessStatusCodeRow row() {
        return new BusinessStatusCodeRow(10L, "DRAFT", "FACULTY_ACHIEVEMENT", "SUBMITTED", "제출", "Y", "표시명 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
