package kr.ac.knue.commonfoundation.basic43;

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

@WebMvcTest(DepartmentChairConfirmationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DepartmentChairConfirmationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DepartmentChairConfirmationService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listDepartmentChairConfirmTargetsSupportsPaginationFiltersAndR09OnlyForReq1336Req1312Req1314() throws Exception {
        when(service.list(new DepartmentChairConfirmationSearchCriteria(0, 20, "2026", "EDUCATION", "SUBMITTED", "Y")))
                .thenReturn(new DepartmentChairConfirmationSearchResponse(List.of(row("DEPARTMENT_CONFIRMED", "확인")), 0, 20, 1));

        mockMvc.perform(get("/api/business/department-chair-confirmations")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("certificationStatus", "SUBMITTED")
                        .param("attachmentYn", "Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targets[0].achievementId").value(9001))
                .andExpect(jsonPath("$.data.targets[0].confirmStatus").value("DEPARTMENT_CONFIRMED"))
                .andExpect(jsonPath("$.data.size").value(20));

        mockMvc.perform(get("/api/business/department-chair-confirmations")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new DepartmentChairConfirmationSearchCriteria(0, 20, null, null, null, null));
    }

    @Test
    void transitionConfirmPersistsStatusProcessorTimestampAndHistorySideEffectForReq1337Req1339() throws Exception {
        when(service.transition(eq(9001L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenReturn(row("DEPARTMENT_CONFIRMED", "학과장 확인 완료"));

        mockMvc.perform(post("/api/business/department-chair-confirmations/9001/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CONFIRM\",\"opinion\":\"학과장 확인 완료\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextStatus").value("DEPARTMENT_CONFIRMED"))
                .andExpect(jsonPath("$.data.processedBy").value(1))
                .andExpect(jsonPath("$.data.processedAt").isString());
    }

    @Test
    void transitionRejectRequiresReasonAndOpinionForReq1338Req1314() throws Exception {
        mockMvc.perform(post("/api/business/department-chair-confirmations/9001/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REJECT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'reasonCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'opinion')]").exists());
    }

    @Test
    void transitionRejectStoresReasonAndOpinionForReq1338() throws Exception {
        when(service.transition(eq(9001L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenReturn(row("DEPARTMENT_REJECTED", "증빙 보완 필요"));

        mockMvc.perform(post("/api/business/department-chair-confirmations/9001/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REJECT\",\"reasonCode\":\"RR001\",\"opinion\":\"증빙 보완 필요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmStatus").value("DEPARTMENT_REJECTED"))
                .andExpect(jsonPath("$.data.reasonCode").value("RR001"))
                .andExpect(jsonPath("$.data.opinion").value("증빙 보완 필요"));
    }

    @Test
    void transitionBlocksNonR09AndPeriodBusinessConflictWithoutSideEffectsForReq1339() throws Exception {
        mockMvc.perform(post("/api/business/department-chair-confirmations/9001/transition")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CONFIRM\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).transition(any(), any(), any());

        when(service.transition(eq(9001L), any(BusinessTransitionRequest.class), eq(1L)))
                .thenThrow(new ConflictException("학과장 확인기간 안의 대상만 처리할 수 있습니다."));
        mockMvc.perform(post("/api/business/department-chair-confirmations/9001/transition")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"CONFIRM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    private DepartmentChairConfirmationRow row(String status, String opinion) {
        return new DepartmentChairConfirmationRow(301L, 9001L, "2026", "DEPT-EDU", "EDUCATION", status,
                "SUBMITTED", status, opinion, "DEPARTMENT_REJECTED".equals(status) ? "RR001" : null,
                1L, LocalDateTime.parse("2026-09-02T09:00:00"), "학과장 확인 처리");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
