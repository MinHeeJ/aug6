package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** XLSX upload endpoints; authorization and owner checks are enforced by the service. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements/excel-uploads")
public class EmploymentRateExcelController {
    private final EmploymentRateExcelService service;

    public EmploymentRateExcelController(EmploymentRateExcelService service) {
        this.service = service;
    }

    /** Downloads the current versioned template. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return binary(service.downloadExcelTemplate(user(request)));
    }

    /** Validates and stages a workbook without writing achievements. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateExcelModels.UploadResult> upload(@RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.validateExcelUpload(file, user(request)), trace(requestId));
    }

    /** Atomically materializes a validated upload after guard rechecks. */
    @PostMapping("/{uploadId}/commit")
    public ApiResponse<EmploymentRateExcelModels.CommitResult> commit(@PathVariable String uploadId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String trace = trace(requestId);
        return ApiResponse.ok(service.commitExcelUpload(uploadId, user(request), trace), trace);
    }

    /** Lists the authenticated operator's retained upload history. */
    @GetMapping("/histories")
    public ApiResponse<List<EmploymentRateExcelModels.HistoryRow>> histories(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.listExcelHistories(user(request)), trace(requestId));
    }

    /** Lists diagnostics only for an upload owned by the operator. */
    @GetMapping("/{uploadId}/errors")
    public ApiResponse<List<EmploymentRateExcelModels.ErrorRow>> errors(@PathVariable String uploadId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.listExcelErrors(uploadId, user(request)), trace(requestId));
    }

    /** Downloads the retained owner-bound error workbook. */
    @GetMapping("/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorsDownload(@PathVariable String uploadId, HttpServletRequest request) {
        return binary(service.downloadExcelErrors(uploadId, user(request)));
    }

    private CurrentUser user(HttpServletRequest request) {
        if (request.getAttribute("currentUser") instanceof CurrentUser user) return user;
        throw new UnauthenticatedException();
    }

    private String trace(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
    }

    private ResponseEntity<byte[]> binary(ExcelDownloadFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
