package kr.ac.knue.commonfoundation.helpcontents;

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
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelpContentManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HelpContentManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean HelpContentManagementService helpContentManagementService;

    @Test
    void listHelpContentsReturnsScreenIdTextAndAuditFields() throws Exception {
        when(helpContentManagementService.listHelpContents(0, 20, "SCR-USER-MGMT")).thenReturn(new HelpContentSearchResponse(
                List.of(helpRow("SCR-USER-MGMT", "사용자 계정 관리", "필수 항목을 입력합니다.", "Q. 저장은 언제 하나요?", "admin@knue.ac.kr")),
                0, 20, 1));

        mockMvc.perform(get("/api/admin/help-contents")
                        .param("screenId", "SCR-USER-MGMT")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.helpContents[0].screenId").value("SCR-USER-MGMT"))
                .andExpect(jsonPath("$.data.helpContents[0].businessDescription").value("사용자 계정 관리"))
                .andExpect(jsonPath("$.data.helpContents[0].inputCriteria").value("필수 항목을 입력합니다."))
                .andExpect(jsonPath("$.data.helpContents[0].faq").value("Q. 저장은 언제 하나요?"))
                .andExpect(jsonPath("$.data.helpContents[0].contact").value("admin@knue.ac.kr"))
                .andExpect(jsonPath("$.data.helpContents[0].createdBy").value(1))
                .andExpect(jsonPath("$.data.helpContents[0].updatedBy").value(1));
    }

    @Test
    void saveHelpContentUpsertsUniqueScreenIdAndBusinessScreenReadsSameScreenOnly() throws Exception {
        when(helpContentManagementService.saveHelpContent(eq("SCR-USER-MGMT"), any(HelpContentSaveRequest.class), eq(1L)))
                .thenReturn(helpRow("SCR-USER-MGMT", "사용자 계정 관리", "필수 항목을 입력합니다.", "Q. 저장은 언제 하나요?", "admin@knue.ac.kr"));
        when(helpContentManagementService.getHelpContent("SCR-USER-MGMT"))
                .thenReturn(new HelpContentResponse("SCR-USER-MGMT", "사용자 계정 관리", "필수 항목을 입력합니다.", "Q. 저장은 언제 하나요?", "admin@knue.ac.kr"));
        when(helpContentManagementService.getHelpContent("SCR-NO-HELP"))
                .thenThrow(new NotFoundException("등록된 도움말이 없습니다."));

        mockMvc.perform(put("/api/admin/help-contents/SCR-USER-MGMT")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessDescription\":\"사용자 계정 관리\",\"inputCriteria\":\"필수 항목을 입력합니다.\",\"faq\":\"Q. 저장은 언제 하나요?\",\"contact\":\"admin@knue.ac.kr\",\"changeReason\":\"도움말 정비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.screenId").value("SCR-USER-MGMT"))
                .andExpect(jsonPath("$.data.businessDescription").value("사용자 계정 관리"));

        mockMvc.perform(get("/api/help-contents/SCR-USER-MGMT").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.screenId").value("SCR-USER-MGMT"))
                .andExpect(jsonPath("$.data.businessDescription").value("사용자 계정 관리"));

        mockMvc.perform(get("/api/help-contents/SCR-NO-HELP").cookie(adminCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void saveHelpContentRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(put("/api/admin/help-contents/SCR-USER-MGMT")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessDescription\":\"사용자 계정 관리\",\"inputCriteria\":\"필수 항목을 입력합니다.\",\"faq\":\"\",\"contact\":\"admin@knue.ac.kr\",\"changeReason\":\"도움말 정비\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void saveHelpContentReturnsFieldErrorsWhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(put("/api/admin/help-contents/SCR-USER-MGMT")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void serviceRejectsUnknownFieldsWithoutMapperSideEffect() {
        HelpContentManagementMapper mapper = mock(HelpContentManagementMapper.class);
        HelpContentManagementService service = new HelpContentManagementService(mapper);
        HelpContentSaveRequest request = new HelpContentSaveRequest();
        request.setBusinessDescription("사용자 계정 관리");
        request.setInputCriteria("필수 항목을 입력합니다.");
        request.setFaq("FAQ");
        request.setContact("admin@knue.ac.kr");
        request.setChangeReason("도움말 정비");
        request.captureUnexpectedField("manualFile", "out-of-scope");

        assertThatThrownBy(() -> service.saveHelpContent("SCR-USER-MGMT", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("도움말");
        verify(mapper, never()).upsertHelpContent(any(), any(), any(), any(), any(), any(), any());
    }

    private HelpContentRow helpRow(String screenId, String businessDescription, String inputCriteria, String faq, String contact) {
        return new HelpContentRow(screenId, businessDescription, inputCriteria, faq, contact,
                LocalDateTime.parse("2026-08-25T09:00:00"), 1L,
                LocalDateTime.parse("2026-08-25T09:30:00"), 1L);
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
