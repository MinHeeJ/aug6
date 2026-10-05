package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP entrypoint for individual, Excel, and bulk employment-rate achievement
 * workflows. Role checks are repeated here before service-side ownership checks.
 */
@RestController
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists employment-rate achievement rows visible to the authenticated caller. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01", "R02", "R04"));
        return ApiResponse.ok(service.list(page, pageSize, user), effectiveRequestId(requestId));
    }

    /** Creates a draft employment-rate achievement for an R01 caller. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementRow> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, user, traceId), traceId);
    }

    /** Returns one caller-authorized employment-rate achievement. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01", "R02", "R04"));
        return ApiResponse.ok(service.get(achievementId, user), effectiveRequestId(requestId));
    }

    /** Updates mutable fields only when the caller owns an unconfirmed achievement. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user, traceId), traceId);
    }

    /** Exports the caller-scoped current page as a spreadsheet-compatible CSV payload. */
    @GetMapping("/api/business/employment-rate-achievements/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R01", "R02", "R04", "R07"));
        EmploymentRateAchievementListResponse response = service.list(page, pageSize, user);
        StringBuilder csv = new StringBuilder(
                "managementNo,managementItemCode,achievementDate,achievementName,status\n");
        response.achievements().forEach(row -> csv.append(row.managementNo()).append(',')
                .append(row.managementItemCode()).append(',')
                .append(row.achievementDate()).append(',')
                .append(escapeCsv(row.achievementName())).append(',')
                .append(row.achievementStatus()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("employment-rate-achievements.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Validates an R07 Excel upload before the shared Excel adapter performs atomic reflection. */
    @PostMapping(
            value = "/api/business/employment-rate-achievements/excel-uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateExcelUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R07"));
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.upload(file, user, traceId), traceId);
    }

    /** Accepts a confirmed R07 preview only after persisting its retrievable job and target rows. */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    public ResponseEntity<ApiResponse<EmploymentRateBulkJobResponse>> createBulkJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R07"));
        String traceId = effectiveRequestId(requestId);
        return ResponseEntity.accepted().body(ApiResponse.ok(service.createBulkJob(body, user, traceId), traceId));
    }

    /** Retrieves an R07 caller's persisted bulk-job outcome and target-level results. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobResponse> getBulkJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireRole(request, List.of("R07"));
        return ApiResponse.ok(service.getBulkJob(jobId, user), effectiveRequestId(requestId));
    }

    private CurrentUser requireRole(HttpServletRequest request, List<String> roles) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(roles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
