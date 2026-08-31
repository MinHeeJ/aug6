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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RejectionReasonController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RejectionReasonApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean RejectionReasonService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listRejectionReasonsReturnsDefaultTwentyAndFiltersForReq774() throws Exception {
        when(service.list(new RejectionReasonSearchCriteria(0, 20, "FACULTY_ACHIEVEMENT", "DEPT", "Y")))
                .thenReturn(new RejectionReasonSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/rejection-reasons")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("businessType", "FACULTY_ACHIEVEMENT")
                        .param("reasonCode", "DEPT")
                        .param("additionalOpinionAllowedYn", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reasons[0].businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.reasons[0].reasonCode").value("DEPT_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.reasons[0].standardMessage").value("학과장 검토 의견이 필요합니다."))
                .andExpect(jsonPath("$.data.reasons[0].additionalOpinionAllowedYn").value("Y"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveRejectionReasonPersistsStandardMessageAndAdditionalOpinionFlagForReq775AndReq776() throws Exception {
        when(service.save(any(RejectionReasonSaveRequest.class), eq(1L))).thenReturn(row());

        mockMvc.perform(post("/api/admin/rejection-reasons")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"FACULTY_ACHIEVEMENT","reasonCode":"DEPT_REVIEW_REQUIRED","standardMessage":"학과장 검토 의견이 필요합니다.","additionalOpinionAllowedYn":"Y","changeReason":"반려사유 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.reasonCode").value("DEPT_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.additionalOpinionAllowedYn").value("Y"));
    }

    @Test
    void saveRejectionReasonRequiresStandardMessageForReq775() throws Exception {
        mockMvc.perform(post("/api/admin/rejection-reasons")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"FACULTY_ACHIEVEMENT","reasonCode":"DEPT_REVIEW_REQUIRED","additionalOpinionAllowedYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("standardMessage"));
    }

    @Test
    void directR01SaveRejectionReasonIsForbiddenForReq775() throws Exception {
        mockMvc.perform(post("/api/admin/rejection-reasons")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessType":"FACULTY_ACHIEVEMENT","reasonCode":"DEPT_REVIEW_REQUIRED","standardMessage":"학과장 검토 의견이 필요합니다.","additionalOpinionAllowedYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any());
    }

    @Test
    void serviceRejectsInvalidBusinessTypeWithoutChangingReasonsForReq775() {
        RejectionReasonMapper mapper = org.mockito.Mockito.mock(RejectionReasonMapper.class);
        RejectionReasonService rejectionReasonService = new RejectionReasonService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rejectionReasonService.save(
                        new RejectionReasonSaveRequest("UNKNOWN", "DEPT_REVIEW_REQUIRED", "학과장 검토 의견이 필요합니다.", "Y", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("반려사유");
        verify(mapper, never()).upsertRejectionReason(any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRequiresAdditionalOpinionYnFlagForReq776() {
        RejectionReasonMapper mapper = org.mockito.Mockito.mock(RejectionReasonMapper.class);
        RejectionReasonService rejectionReasonService = new RejectionReasonService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rejectionReasonService.save(
                        new RejectionReasonSaveRequest("FACULTY_ACHIEVEMENT", "DEPT_REVIEW_REQUIRED", "학과장 검토 의견이 필요합니다.", "INVALID", "검증"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("반려사유");
        verify(mapper, never()).upsertRejectionReason(any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRecordsRejectionReasonChangeHistoryForReq776() {
        RejectionReasonMapper mapper = org.mockito.Mockito.mock(RejectionReasonMapper.class);
        RejectionReasonService rejectionReasonService = new RejectionReasonService(mapper);
        RejectionReasonRow before = new RejectionReasonRow(10L, "FACULTY_ACHIEVEMENT", "DEPT_REVIEW_REQUIRED", "기존 문구", "N", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        RejectionReasonRow after = new RejectionReasonRow(10L, "FACULTY_ACHIEVEMENT", "DEPT_REVIEW_REQUIRED", "학과장 검토 의견이 필요합니다.", "Y", "반려사유 정비", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findByKey("FACULTY_ACHIEVEMENT", "DEPT_REVIEW_REQUIRED")).thenReturn(before, after);

        rejectionReasonService.save(new RejectionReasonSaveRequest("FACULTY_ACHIEVEMENT", "DEPT_REVIEW_REQUIRED", "학과장 검토 의견이 필요합니다.", "Y", "반려사유 정비"), 1L);

        verify(mapper).insertChangeHistory("rejection_reasons", "FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED", "UPDATE", "standard_message", "기존 문구", "학과장 검토 의견이 필요합니다.", 1L, "반려사유 정비");
        verify(mapper).insertChangeHistory("rejection_reasons", "FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED", "UPDATE", "additional_opinion_allowed_yn", "N", "Y", 1L, "반려사유 정비");
    }

    private RejectionReasonRow row() {
        return new RejectionReasonRow(10L, "FACULTY_ACHIEVEMENT", "DEPT_REVIEW_REQUIRED", "학과장 검토 의견이 필요합니다.", "Y", "반려사유 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
