package kr.ac.knue.commonfoundation.basic70;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic60.Basic60Controller;
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the three BASIC-70 current-filter Excel download HTTP contracts before the
 * operational-settings controller exposes the binary exports.
 */
@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic70ExcelDownloadApiTest {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Autowired MockMvc mockMvc;
    @MockBean Basic60Service service;

    private final CurrentUser businessAdmin = new CurrentUser(
            4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser teacher = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void elementSettingsDownloadReturnsAnAttachmentForTheCurrentFilterAndPageSize() throws Exception {
        expectExcelDownload(get("/api/admin/evaluation-element-management-item-settings/download")
                .param("areaCode", "EDUCATION")
                .param("page", "0")
                .param("pageSize", "50"));
    }

    @Test
    void participationSettingsDownloadReturnsAnAttachmentForTheCurrentFilterAndPageSize() throws Exception {
        expectExcelDownload(get("/api/admin/participation-allocation-rate-settings/download")
                .param("managementItemCode", "PAPER_SCORE")
                .param("page", "1")
                .param("pageSize", "100"));
    }

    @Test
    void scoreSettingsDownloadReturnsAnAttachmentForTheCurrentFilterAndPageSize() throws Exception {
        expectExcelDownload(get("/api/admin/management-item-evaluation-score-settings/download")
                .param("organizationCode", "COL-EDU")
                .param("page", "0")
                .param("pageSize", "20"));
    }

    @Test
    void settingsDownloadsRejectUsersOutsideTheR04AndR09Scope() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-element-management-item-settings/download")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .param("pageSize", "20"))
                .andExpect(status().isForbidden());
    }

    private void expectExcelDownload(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.requestAttr("currentUser", businessAdmin)
                .cookie(sessionCookie())
                .header("X-Request-Id", "REQ-B70-EXCEL-DOWNLOAD"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}
