package kr.ac.knue.commonfoundation.notices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticeManagementController {
    private final NoticeManagementService service;

    public NoticeManagementController(NoticeManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/notices")
    public ApiResponse<NoticeSearchResponse> listNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishEndDate,
            @RequestParam(required = false) String targetRoleCode,
            @RequestParam(required = false) String targetOrganizationCode,
            @RequestParam(defaultValue = "true") Boolean activeOnly) {
        return ApiResponse.ok(service.listNotices(page, pageSize,
                new NoticeSearchCriteria(publishStartDate, publishEndDate, targetRoleCode, targetOrganizationCode, activeOnly)));
    }

    @PostMapping("/api/admin/notices")
    public ApiResponse<NoticeRow> createNotice(@Valid @RequestBody NoticeSaveRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createNotice(request, currentUser(servletRequest).userId()));
    }

    @PutMapping("/api/admin/notices/{noticeId}")
    public ApiResponse<NoticeRow> saveNotice(@PathVariable Long noticeId, @Valid @RequestBody NoticeSaveRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.saveNotice(noticeId, request, currentUser(servletRequest).userId()));
    }

    @GetMapping("/api/admin/notices/{noticeId}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadNoticeAttachment(@PathVariable Long noticeId, @PathVariable Long attachmentId,
            @RequestParam(required = false) String organizationCode, HttpServletRequest servletRequest) {
        CurrentUser user = currentUser(servletRequest);
        NoticeAttachmentDownload download = service.downloadAttachment(noticeId, attachmentId, user.roles(), organizationCode);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .body(download.fileContent());
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
