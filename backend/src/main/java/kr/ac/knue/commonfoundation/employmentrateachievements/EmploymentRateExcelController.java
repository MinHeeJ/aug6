package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Feature-owned D6 endpoints. All entry points require R07, including retained diagnostics. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements/excel-uploads")
public class EmploymentRateExcelController {
    private final EmploymentRateExcelService service;

    public EmploymentRateExcelController(EmploymentRateExcelService service) {
        this.service = service;
    }

    public record CommitRequest(Boolean confirmed) { }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return binary(service.downloadExcelTemplate(user(request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EmploymentRateExcelService.UploadResult>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String trace = trace(requestId);
        request.setAttribute("requestId", trace);
        var result = service.validateExcelUpload(file, user(request), trace);
        ApiResponse<EmploymentRateExcelService.UploadResult> envelope = ApiResponse.ok(result, trace);
        if (result.errors().isEmpty()) return ResponseEntity.ok(envelope);
        List<ValidationError> fields = result.errors().stream()
                .map(error -> new ValidationError("rows[" + error.rowNumber() + "]." + error.columnName(),
                        error.errorCode() + ": " + error.errorReason())).toList();
        // Diagnostics already committed in their own transaction; returning 400 must not roll them back.
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, result,
                new ApiError("VALIDATION_ERROR", "오류 또는 중복 행이 있어 0건 반영했습니다.", fields), envelope.meta()));
    }

    @PostMapping("/{uploadId}/commit")
    public ApiResponse<EmploymentRateExcelService.CommitResult> commit(
            @PathVariable String uploadId,
            @RequestBody(required = false) CommitRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = user(request);
        String trace = trace(requestId);
        request.setAttribute("requestId", trace);
        if (body == null || !Boolean.TRUE.equals(body.confirmed())) {
            throw new ConflictException("CONFIRMATION_REQUIRED: 검증결과 확인 후 반영을 승인하세요.");
        }
        return ApiResponse.ok(service.commitExcelUpload(uploadId, user, true, trace), trace);
    }

    @GetMapping("/histories")
    public ApiResponse<List<Map<String, Object>>> histories(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.listExcelHistories(user(request)), trace(requestId));
    }

    @GetMapping("/{uploadId}/errors")
    public ApiResponse<List<EmploymentRateExcelService.Error>> errors(
            @PathVariable String uploadId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.listExcelErrors(uploadId, user(request)), trace(requestId));
    }

    @GetMapping("/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorDownload(@PathVariable String uploadId, HttpServletRequest request) {
        return binary(service.downloadExcelErrors(uploadId, user(request)));
    }

    private CurrentUser user(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) throw new UnauthenticatedException();
        if (user.roles() == null || !user.roles().contains("R07")) throw new ForbiddenException();
        return user;
    }

    private ResponseEntity<byte[]> binary(ExcelDownloadFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }

    private String trace(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
    }
}
