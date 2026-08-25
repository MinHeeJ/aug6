package kr.ac.knue.commonfoundation.messages;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MessageManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean MessageManagementService messageManagementService;

    @Test
    void listMessagesReturnsMessageCodeTypeUserMessageAndAuditFields() throws Exception {
        when(messageManagementService.listMessages(0, 20, "SAVE", "저장")).thenReturn(new MessageSearchResponse(
                List.of(new MessageCodeRow("SAVE.SUCCESS", "SAVE", "저장되었습니다.",
                        LocalDateTime.parse("2026-08-25T09:00:00"), 1L,
                        LocalDateTime.parse("2026-08-25T09:30:00"), 1L)), 0, 20, 1));

        mockMvc.perform(get("/api/admin/system-settings/messages")
                        .param("messageType", "SAVE")
                        .param("messageCode", "저장")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].messageCode").value("SAVE.SUCCESS"))
                .andExpect(jsonPath("$.data.messages[0].messageType").value("SAVE"))
                .andExpect(jsonPath("$.data.messages[0].userMessage").value("저장되었습니다."))
                .andExpect(jsonPath("$.data.messages[0].createdBy").value(1))
                .andExpect(jsonPath("$.data.messages[0].updatedBy").value(1));
    }

    @Test
    void createMessageRequiresAuthValidatesBodyAndPersistsTableAuditSideEffect() throws Exception {
        when(messageManagementService.createMessage(any(MessageSaveRequest.class), eq(1L)))
                .thenReturn(new MessageCodeRow("ERROR.TIMEOUT", "ERROR", "처리 시간이 초과되었습니다.",
                        LocalDateTime.parse("2026-08-25T09:00:00"), 1L,
                        LocalDateTime.parse("2026-08-25T09:30:00"), 1L));

        mockMvc.perform(post("/api/admin/system-settings/messages")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageCode\":\"ERROR.TIMEOUT\",\"messageType\":\"ERROR\",\"userMessage\":\"처리 시간이 초과되었습니다.\",\"changeReason\":\"오류 문구 등록\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCode").value("ERROR.TIMEOUT"))
                .andExpect(jsonPath("$.data.messageType").value("ERROR"))
                .andExpect(jsonPath("$.data.userMessage").value("처리 시간이 초과되었습니다."))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.updatedBy").value(1));
        verify(messageManagementService).createMessage(any(MessageSaveRequest.class), eq(1L));
    }

    @Test
    void createMessageReturnsValidationErrorAndDoesNotWriteWhenMessageCodeIsMissing() throws Exception {
        when(messageManagementService.createMessage(any(MessageSaveRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("메시지 저장 요청이 올바르지 않습니다.",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("messageCode", "메시지코드를 입력하세요."))));

        mockMvc.perform(post("/api/admin/system-settings/messages")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"ERROR\",\"userMessage\":\"처리 시간이 초과되었습니다.\",\"changeReason\":\"오류 문구 등록\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(messageManagementService).createMessage(any(MessageSaveRequest.class), eq(1L));
    }

    @Test
    void saveMessageUpsertsCodeAndLatestTextIsReturnedByBusinessScreens() throws Exception {
        when(messageManagementService.saveMessage(eq("SAVE.SUCCESS"), any(MessageSaveRequest.class), eq(1L)))
                .thenReturn(new MessageCodeRow("SAVE.SUCCESS", "SAVE", "저장되었습니다.",
                        LocalDateTime.parse("2026-08-25T09:00:00"), 1L,
                        LocalDateTime.parse("2026-08-25T09:30:00"), 1L));
        when(messageManagementService.getMessageText("SAVE.SUCCESS"))
                .thenReturn(new MessageTextResponse("SAVE.SUCCESS", "저장되었습니다."));

        mockMvc.perform(put("/api/admin/system-settings/messages/SAVE.SUCCESS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"SAVE\",\"userMessage\":\"저장되었습니다.\",\"changeReason\":\"저장 문구 정비\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCode").value("SAVE.SUCCESS"))
                .andExpect(jsonPath("$.data.userMessage").value("저장되었습니다."));

        mockMvc.perform(get("/api/system/messages/SAVE.SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCode").value("SAVE.SUCCESS"))
                .andExpect(jsonPath("$.data.userMessage").value("저장되었습니다."));
    }

    @Test
    void saveMessageRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/messages/SAVE.SUCCESS")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"SAVE\",\"userMessage\":\"저장되었습니다.\",\"changeReason\":\"저장 문구 정비\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void saveMessageReturnsFieldErrorsWhenMandatoryFieldsAreMissing() throws Exception {
        mockMvc.perform(put("/api/admin/system-settings/messages/SAVE.SUCCESS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void serviceRejectsInvalidMessageTypeWithoutMapperSideEffect() {
        MessageManagementMapper mapper = mock(MessageManagementMapper.class);
        MessageManagementService service = new MessageManagementService(mapper);
        MessageSaveRequest request = new MessageSaveRequest();
        request.setMessageType("NOTICE");
        request.setUserMessage("알 수 없는 유형입니다.");
        request.setChangeReason("유형 검증");

        assertThatThrownBy(() -> service.saveMessage("SAVE.SUCCESS", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("메시지");
        verify(mapper, never()).upsertMessage(any(), any(), any(), any(), any());
    }

    @Test
    void serviceRejectsCreateMessageWithoutMessageCodeBeforeMapperSideEffect() {
        MessageManagementMapper mapper = mock(MessageManagementMapper.class);
        MessageManagementService service = new MessageManagementService(mapper);
        MessageSaveRequest request = new MessageSaveRequest();
        request.setMessageType("ERROR");
        request.setUserMessage("처리 시간이 초과되었습니다.");
        request.setChangeReason("오류 문구 등록");

        assertThatThrownBy(() -> service.createMessage(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("메시지");
        verify(mapper, never()).upsertMessage(any(), any(), any(), any(), any());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
