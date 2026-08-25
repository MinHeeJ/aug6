package kr.ac.knue.commonfoundation.manuals;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ManualManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ManualManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ManualManagementService manualManagementService;

    @Test
    void listManualsMarksLatestEffectiveVersionAndKeepsPreviousVersions() throws Exception {
        when(manualManagementService.listManuals(0, 20, "USER", "R09", LocalDate.parse("2026-08-25")))
                .thenReturn(new ManualSearchResponse(List.of(
                        new ManualRow(202L, "USER", "1.1", "R09", LocalDate.parse("2026-08-01"),
                                "user-manual-v1.1.txt", true, LocalDateTime.parse("2026-08-01T09:00:00"), 1L,
                                LocalDateTime.parse("2026-08-01T09:00:00"), 1L),
                        new ManualRow(201L, "USER", "1.0", "R09", LocalDate.parse("2026-01-01"),
                                "user-manual-v1.txt", false, LocalDateTime.parse("2026-01-01T09:00:00"), 1L,
                                LocalDateTime.parse("2026-01-01T09:00:00"), 1L)), 0, 20, 2));

        mockMvc.perform(get("/api/admin/manuals")
                        .param("manualType", "USER")
                        .param("targetUser", "R09")
                        .param("effectiveDate", "2026-08-25")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manuals[0].manualId").value(202))
                .andExpect(jsonPath("$.data.manuals[0].latest").value(true))
                .andExpect(jsonPath("$.data.manuals[1].version").value("1.0"))
                .andExpect(jsonPath("$.data.manuals[1].latest").value(false));
    }

    @Test
    void createManualPersistsVersionTargetEffectiveDateAndOriginalFileName() throws Exception {
        when(manualManagementService.createManual(any(ManualCreateRequest.class), eq(1L)))
                .thenReturn(new ManualRow(203L, "ADMIN", "2.0", "R09", LocalDate.parse("2026-09-01"),
                        "admin-manual.txt", true, LocalDateTime.parse("2026-08-25T09:00:00"), 1L,
                        LocalDateTime.parse("2026-08-25T09:00:00"), 1L));

        mockMvc.perform(post("/api/admin/manuals")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"manualType":"ADMIN","version":"2.0","targetUser":"R09","effectiveDate":"2026-09-01","originalFileName":"admin-manual.txt","fileContent":"manual body","changeReason":"관리자 매뉴얼 등록"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manualType").value("ADMIN"))
                .andExpect(jsonPath("$.data.version").value("2.0"))
                .andExpect(jsonPath("$.data.originalFileName").value("admin-manual.txt"));
    }

    @Test
    void createManualReturnsFieldErrorsWhenRequiredFileFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/admin/manuals")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"manualType":"USER","version":"1.0"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields").isArray());
    }

    @Test
    void createManualRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(post("/api/admin/manuals")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"manualType":"USER","version":"1.0","targetUser":"R09","effectiveDate":"2026-09-01","originalFileName":"user.txt","fileContent":"body","changeReason":"등록"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void downloadManualFileUsesOriginalFileNameAndDoesNotExposeInternalStorageName() throws Exception {
        when(manualManagementService.downloadManualFile(203L))
                .thenReturn(new ManualDownload(203L, "admin-manual.txt", "manual body".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/admin/manuals/203/download")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-manual.txt\""))
                .andExpect(content().bytes("manual body".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void serviceRejectsDuplicateManualTypeTargetVersionWithoutMapperSideEffect() {
        ManualManagementMapper mapper = mock(ManualManagementMapper.class);
        ManualManagementService service = new ManualManagementService(mapper);
        ManualCreateRequest request = validCreateRequest();
        when(mapper.findDuplicate("USER", "R09", "1.0"))
                .thenReturn(new ManualRow(201L, "USER", "1.0", "R09", LocalDate.parse("2026-01-01"),
                        "user-manual-v1.txt", false, LocalDateTime.now(), 1L, LocalDateTime.now(), 1L));

        assertThatThrownBy(() -> service.createManual(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("동일 유형");
        verify(mapper, never()).insertManual(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertManualFile(any(), any(), any(), any(), any());
    }

    private ManualCreateRequest validCreateRequest() {
        ManualCreateRequest request = new ManualCreateRequest();
        request.setManualType("USER");
        request.setVersion("1.0");
        request.setTargetUser("R09");
        request.setEffectiveDate(LocalDate.parse("2026-09-01"));
        request.setOriginalFileName("user-manual.txt");
        request.setFileContent("manual body");
        request.setChangeReason("매뉴얼 등록");
        return request;
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
