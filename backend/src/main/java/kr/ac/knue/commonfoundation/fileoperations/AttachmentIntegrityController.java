package kr.ac.knue.commonfoundation.fileoperations;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttachmentIntegrityController {
    private final AttachmentIntegrityService service;

    public AttachmentIntegrityController(AttachmentIntegrityService service) {
        this.service = service;
    }

    @PostMapping("/api/admin/attachment-integrity-checks")
    public ApiResponse<AttachmentIntegrityCheckResponse> createAttachmentIntegrityCheck(HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.createCheck(currentUser(servletRequest)));
    }

    @GetMapping("/api/admin/attachment-integrity-results")
    public ApiResponse<AttachmentIntegrityResultSearchResponse> listAttachmentIntegrityResults(
            @RequestParam(required = false) Long checkId,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.listResults(checkId, anomalyType, page, size, currentUser(servletRequest)));
    }

    @GetMapping("/api/admin/attachment-integrity-results/excel")
    public ResponseEntity<byte[]> downloadAttachmentIntegrityExcel(
            @RequestParam(required = false) Long checkId,
            @RequestParam(required = false) String anomalyType,
            HttpServletRequest servletRequest) {
        AttachmentIntegrityExcelDownload download = service.downloadExcel(checkId, anomalyType, currentUser(servletRequest));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
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
