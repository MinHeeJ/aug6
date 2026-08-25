package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.helpcontents.HelpContentManagementController;
import kr.ac.knue.commonfoundation.helpcontents.HelpContentManagementService;
import kr.ac.knue.commonfoundation.helpcontents.HelpContentSearchResponse;
import kr.ac.knue.commonfoundation.manuals.ManualManagementController;
import kr.ac.knue.commonfoundation.manuals.ManualManagementService;
import kr.ac.knue.commonfoundation.manuals.ManualSearchResponse;
import kr.ac.knue.commonfoundation.messages.MessageManagementController;
import kr.ac.knue.commonfoundation.messages.MessageManagementService;
import kr.ac.knue.commonfoundation.messages.MessageSearchResponse;
import kr.ac.knue.commonfoundation.notices.NoticeManagementController;
import kr.ac.knue.commonfoundation.notices.NoticeManagementService;
import kr.ac.knue.commonfoundation.notices.NoticeSearchCriteria;
import kr.ac.knue.commonfoundation.notices.NoticeSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        MessageManagementController.class,
        NoticeManagementController.class,
        HelpContentManagementController.class,
        ManualManagementController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic22CrossCuttingVerificationTest {
    @Autowired MockMvc mockMvc;
    @MockBean MessageManagementService messageManagementService;
    @MockBean NoticeManagementService noticeManagementService;
    @MockBean HelpContentManagementService helpContentManagementService;
    @MockBean ManualManagementService manualManagementService;

    @Test
    void allNewListApisDefaultToTwentyItemsWhenSizeIsOmitted() throws Exception {
        when(messageManagementService.listMessages(0, 20, null, null))
                .thenReturn(new MessageSearchResponse(List.of(), 0, 20, 0));
        when(noticeManagementService.listNotices(eq(0), eq(20), any(NoticeSearchCriteria.class)))
                .thenReturn(new NoticeSearchResponse(List.of(), 0, 20, 0));
        when(helpContentManagementService.listHelpContents(0, 20, null))
                .thenReturn(new HelpContentSearchResponse(List.of(), 0, 20, 0));
        when(manualManagementService.listManuals(0, 20, null, null, null))
                .thenReturn(new ManualSearchResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/admin/system-settings/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(20));
        mockMvc.perform(get("/api/admin/help-contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
        mockMvc.perform(get("/api/admin/manuals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void allNewListApisAcceptTheContractedTwentyFiftyOneHundredPageSizes() throws Exception {
        when(messageManagementService.listMessages(0, 50, null, null))
                .thenReturn(new MessageSearchResponse(List.of(), 0, 50, 0));
        when(noticeManagementService.listNotices(eq(0), eq(100), any(NoticeSearchCriteria.class)))
                .thenReturn(new NoticeSearchResponse(List.of(), 0, 100, 0));
        when(helpContentManagementService.listHelpContents(0, 50, null))
                .thenReturn(new HelpContentSearchResponse(List.of(), 0, 50, 0));
        when(manualManagementService.listManuals(0, 100, null, null, null))
                .thenReturn(new ManualSearchResponse(List.of(), 0, 100, 0));

        mockMvc.perform(get("/api/admin/system-settings/messages").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
        mockMvc.perform(get("/api/admin/notices").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(100));
        mockMvc.perform(get("/api/admin/help-contents").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(50));
        mockMvc.perform(get("/api/admin/manuals").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));

        verify(messageManagementService).listMessages(0, 50, null, null);
        verify(noticeManagementService).listNotices(eq(0), eq(100), any(NoticeSearchCriteria.class));
        verify(helpContentManagementService).listHelpContents(0, 50, null);
        verify(manualManagementService).listManuals(0, 100, null, null, null);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void unexpectedErrorsReturnUserSafeGuidanceWhileSystemDetailsStayInLogs(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleUnexpectedError(new IllegalStateException("jdbc password=secret failed"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo("오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요.");
        assertThat(response.getBody().error().message()).doesNotContain("jdbc", "password", "secret", "IllegalStateException");
        assertThat(output).contains("Unexpected system error").contains("jdbc password=secret failed");
    }
}
