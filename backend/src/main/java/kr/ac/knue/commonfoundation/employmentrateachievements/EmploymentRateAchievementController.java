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
 * Owns the approved employment-rate achievement HTTP contract, separating
 * individual R01/R02/R04 access from R07-only upload and batch operations.
 */
@RestController
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists caller-scoped employment-rate achievements with the contract page sizes. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.list(page, pageSize, achievementUser(request)), traceId);
    }

    /** Creates a DRAFT employment-rate achievement and its associated audit entries. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSaveResponse> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, createUser(request), traceId), traceId);
    }

    /** Retrieves one authorized employment-rate achievement by its durable identity. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.get(achievementId, achievementUser(request)), traceId);
    }

    /** Updates an R01-owned record only while the shared evaluation lock permits mutation. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementSaveResponse> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, createUser(request), traceId), traceId);
    }

    /** Exports the caller-authorized result list using a download attachment response. */
    @GetMapping("/api/business/employment-rate-achievements/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        byte[] content = service.download(page, pageSize, downloadUser(request));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("취업률실적.csv", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(content);
    }

    /** Validates all rows before atomically applying a successful R07 upload. */
    @PostMapping(
            value = "/api/business/employment-rate-achievements/excel-uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateExcelUploadResult> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.upload(file, excelUser(request), traceId), traceId);
    }

    /** Enforces the R07 boundary before the unresolved bulk policy reaches the service. */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    public ResponseEntity<ApiResponse<EmploymentRateBulkJobResponse>> createBulkJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        EmploymentRateBulkJobResponse result = service.createBulkJob(body, excelUser(request), traceId);
        return ResponseEntity.accepted().body(ApiResponse.ok(result, traceId));
    }

    /** Returns the R07-visible status and item counts for an existing batch job. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobResponse> getBulkJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.getBulkJob(jobId, excelUser(request)), traceId);
    }

    private CurrentUser achievementUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(
                        role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser createUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser downloadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R07").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser excelUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R07")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
