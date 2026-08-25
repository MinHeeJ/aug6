package kr.ac.knue.commonfoundation.notices;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NoticeManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NoticeManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean NoticeManagementService noticeManagementService;

    @Test
    void listNoticesReturnsOnlyPeriodRoleAndOrganizationMatchedNoticesWithAttachmentsHiddenInternalNames() throws Exception {
        NoticeSearchResponse response = new NoticeSearchResponse(List.of(noticeSummary()), 0, 20, 1);
        when(noticeManagementService.listNotices(eq(0), eq(20), any(NoticeSearchCriteria.class))).thenReturn(response);

        mockMvc.perform(get("/api/admin/notices")
                        .param("targetRoleCode", "R09")
                        .param("targetOrganizationCode", "ORG001-CHILD")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notices[0].title").value("시스템 점검 안내"))
                .andExpect(jsonPath("$.data.notices[0].targets[0].targetType").value("ROLE"))
                .andExpect(jsonPath("$.data.notices[0].targets[1].targetIdValue").value("ORG001"))
                .andExpect(jsonPath("$.data.notices[0].attachments[0].originalFileName").value("점검안내.txt"))
                .andExpect(jsonPath("$.data.notices[0].attachments[0].storedFileName").doesNotExist());
    }

    @Test
    void createNoticePersistsTargetsPeriodAndAttachmentOriginalFileName() throws Exception {
        when(noticeManagementService.createNotice(any(NoticeSaveRequest.class), eq(1L))).thenReturn(noticeRow());

        mockMvc.perform(post("/api/admin/notices")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNoticeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeId").value(11))
                .andExpect(jsonPath("$.data.targets[0].targetIdValue").value("R09"))
                .andExpect(jsonPath("$.data.attachments[0].originalFileName").value("점검안내.txt"));
    }

    @Test
    void saveNoticeRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(put("/api/admin/notices/11")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNoticeJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void saveNoticeReturnsFieldErrorsWhenRequiredTargetsAreMissing() throws Exception {
        when(noticeManagementService.createNotice(any(NoticeSaveRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("공지사항 저장 요청이 올바르지 않습니다.",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("targets", "대상 역할 또는 대상 조직을 하나 이상 지정하세요."))));

        mockMvc.perform(post("/api/admin/notices")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"본문\",\"publishStartDate\":\"2026-08-25\",\"publishEndDate\":\"2026-08-31\",\"importantYn\":\"Y\",\"targets\":[],\"changeReason\":\"공지 등록\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void downloadNoticeAttachmentRevalidatesPermissionAndUsesOriginalFileNameOnly() throws Exception {
        when(noticeManagementService.downloadAttachment(11L, 101L, List.of("R09"), "ORG001-CHILD"))
                .thenReturn(new NoticeAttachmentDownload(101L, 11L, "점검안내.txt", "첨부 내용".getBytes()));

        mockMvc.perform(get("/api/admin/notices/11/attachments/101/download")
                        .requestAttr("currentUser", adminUser())
                        .param("organizationCode", "ORG001-CHILD")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("seed-notice-maintenance"))));
    }

    @Test
    void serviceRejectsOutOfOrderPeriodAndDoesNotWriteNotice() {
        NoticeManagementMapper mapper = mock(NoticeManagementMapper.class);
        NoticeManagementService service = new NoticeManagementService(mapper);
        NoticeSaveRequest request = validRequest();
        request.setPublishStartDate(LocalDate.parse("2026-09-01"));
        request.setPublishEndDate(LocalDate.parse("2026-08-31"));

        assertThatThrownBy(() -> service.createNotice(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("공지사항");
        verify(mapper, never()).insertNotice(any(), any(), any());
    }

    @Test
    void serviceRejectsRequiredRoleOrOrganizationTargetMissing() {
        NoticeManagementMapper mapper = mock(NoticeManagementMapper.class);
        NoticeManagementService service = new NoticeManagementService(mapper);
        NoticeSaveRequest request = validRequest();
        NoticeTargetInput roleOnly = new NoticeTargetInput();
        roleOnly.setTargetType("ROLE");
        roleOnly.setTargetId("R09");
        request.setTargets(List.of(roleOnly));

        assertThatThrownBy(() -> service.createNotice(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("공지사항");
        verify(mapper, never()).insertNotice(any(), any(), any());
    }

    private NoticeSummaryRow noticeSummary() {
        return new NoticeSummaryRow(11L, "시스템 점검 안내", "점검 본문", LocalDate.parse("2026-08-25"),
                LocalDate.parse("2026-08-31"), "Y", "ACTIVE", LocalDateTime.parse("2026-08-25T10:00:00"), 1L,
                targets(), attachments());
    }

    private NoticeRow noticeRow() {
        return new NoticeRow(11L, "시스템 점검 안내", "점검 본문", LocalDate.parse("2026-08-25"),
                LocalDate.parse("2026-08-31"), "Y", "ACTIVE", LocalDateTime.parse("2026-08-25T09:00:00"), 1L,
                LocalDateTime.parse("2026-08-25T10:00:00"), 1L, targets(), attachments());
    }

    private List<NoticeTargetRow> targets() {
        return List.of(new NoticeTargetRow(1L, 11L, "ROLE", "R09", "시스템관리자"),
                new NoticeTargetRow(2L, 11L, "ORGANIZATION", "ORG001", "예시 조직"));
    }

    private List<NoticeAttachmentRow> attachments() {
        return List.of(new NoticeAttachmentRow(101L, 11L, "점검안내.txt", 12L, LocalDateTime.parse("2026-08-25T09:00:00"), 1L));
    }

    private String validNoticeJson() {
        return "{\"title\":\"시스템 점검 안내\",\"content\":\"점검 본문\",\"publishStartDate\":\"2026-08-25\",\"publishEndDate\":\"2026-08-31\",\"importantYn\":\"Y\",\"targets\":[{\"targetType\":\"ROLE\",\"targetId\":\"R09\"},{\"targetType\":\"ORGANIZATION\",\"targetId\":\"ORG001\"}],\"attachments\":[{\"originalFileName\":\"점검안내.txt\",\"contentText\":\"첨부 내용\"}],\"changeReason\":\"공지 등록\"}";
    }

    private NoticeSaveRequest validRequest() {
        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setTitle("시스템 점검 안내");
        request.setContent("점검 본문");
        request.setPublishStartDate(LocalDate.parse("2026-08-25"));
        request.setPublishEndDate(LocalDate.parse("2026-08-31"));
        request.setImportantYn("Y");
        request.setChangeReason("공지 등록");
        NoticeTargetInput role = new NoticeTargetInput();
        role.setTargetType("ROLE");
        role.setTargetId("R09");
        NoticeTargetInput organization = new NoticeTargetInput();
        organization.setTargetType("ORGANIZATION");
        organization.setTargetId("ORG001");
        request.setTargets(List.of(role, organization));
        return request;
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }
}
