package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP boundary for employment-rate export, Excel ingestion, and R07 bulk-job
 * operations. It keeps the binary export contract separate from JSON envelopes.
 */
@RestController
public class EmploymentRateAchievementOperationsController {
    private static final Set<String> EXPORT_ROLES = Set.of("R01", "R02", "R04", "R07");
    private final EmploymentRateAchievementOperationsService service;

    public EmploymentRateAchievementOperationsController(
            EmploymentRateAchievementOperationsService service) {
        this.service = service;
    }

    /** Downloads the caller-scoped employment-rate result set as an XLSX workbook. */
    @GetMapping("/api/business/employment-rate-achievements/download")
    public ResponseEntity<byte[]> downloadEmploymentRateAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        byte[] workbook = service.download(page, pageSize, requireRole(request, EXPORT_ROLES));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("employment-rate-achievements.xlsx")
                        .build()
                        .toString())
                .header("X-Request-Id", traceId)
                .body(workbook);
    }

    /** Validates and atomically applies an R07 employment-rate Excel upload. */
    @PostMapping(
            value = "/api/business/employment-rate-achievements/excel-uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateExcelUploadResult> uploadEmploymentRateAchievementsExcel(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        return ApiResponse.ok(service.upload(file, requireRole(request, Set.of("R07"))));
    }

    /**
     * Rejects unapproved bulk execution conditions until OQ-83-01 has an approved
     * policy; the service guarantees that no bulk-job row is created on this path.
     */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    public ResponseEntity<ApiResponse<EmploymentRateBulkJobResult>> createEmploymentRateBulkJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        EmploymentRateBulkJobResult result = service.createBulkJob(body, requireRole(request, Set.of("R07")));
        return ResponseEntity.accepted().body(ApiResponse.ok(result, traceId));
    }

    /** Reads an R07 bulk-job result without exposing a job owned by another process. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobResult> getEmploymentRateBulkJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.getBulkJob(jobId, requireRole(request, Set.of("R07"))), traceId);
    }

    private CurrentUser requireRole(HttpServletRequest request, Set<String> allowedRoles) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(allowedRoles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
