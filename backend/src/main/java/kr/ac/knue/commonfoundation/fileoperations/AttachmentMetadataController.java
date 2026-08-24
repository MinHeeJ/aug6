package kr.ac.knue.commonfoundation.fileoperations;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttachmentMetadataController {
    private final AttachmentMetadataService service;

    public AttachmentMetadataController(AttachmentMetadataService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/attachments")
    public ApiResponse<AttachmentSearchResponse> listAttachments(
            @RequestParam String businessRecordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.listAttachments(businessRecordId, page, size, currentUser(servletRequest)));
    }

    @GetMapping("/api/admin/attachments/{fileId}/download")
    public ResponseEntity<byte[]> getAttachmentDownload(
            @PathVariable Long fileId,
            HttpServletRequest servletRequest) {
        AttachmentDownloadResponse download = service.downloadAttachment(fileId, currentUser(servletRequest));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
