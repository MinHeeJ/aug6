package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
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
 * HTTP boundary for the BASIC-83 employment-rate achievement list, detail,
 * individual save, Excel, download, and policy-gated batch operations.
 */
@RestController
public class EmploymentRateAchievementController {
    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists scoped employment-rate achievements with the configured page sizes. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePage(page, pageSize);
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R01", "R02", "R04");
        return ApiResponse.ok(
                service.list(user, page, pageSize, managementItemCode, achievementStatus),
                requestId(requestId));
    }

    /** Reads a scoped individual employment-rate achievement. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R01", "R02", "R04");
        return ApiResponse.ok(service.get(achievementId, user), requestId(requestId));
    }

    /** Creates a draft employment-rate achievement as an R01 faculty user. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSaveResponse> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = requestId(requestId);
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R01");
        return ApiResponse.ok(service.create(body, user, traceId), traceId);
    }

    /** Updates an owned employment-rate achievement while preserving its lifecycle identity. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementSaveResponse> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = requestId(requestId);
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R01");
        return ApiResponse.ok(service.update(achievementId, body, user, traceId), traceId);
    }

    /** Returns a caller-scoped CSV download derived from the achievement list data. */
    @GetMapping("/api/business/employment-rate-achievements/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        validatePage(page, pageSize);
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R01", "R02", "R04");
        byte[] bytes = service.download(user, page, pageSize);
        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("employment-rate-achievements.csv")
                                .build()
                                .toString())
                .body(bytes);
    }

    /** Accepts an R07 Excel file for all-row validation through the shared upload adapter. */
    @PostMapping(
            value = "/api/business/employment-rate-achievements/excel-uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateExcelUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = requestId(requestId);
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R07");
        return ApiResponse.ok(service.upload(file, user, traceId), traceId);
    }

    /** Rejects unapproved batch execution rather than inventing OQ-83-01 policy. */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    public ResponseEntity<ApiResponse<EmploymentRateBulkJobResponse>> createBulkJob(
            @RequestBody EmploymentRateBulkJobRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = requestId(requestId);
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R07");
        EmploymentRateBulkJobResponse result = service.createBulkJob(body, user, traceId);
        return ResponseEntity.accepted().body(ApiResponse.ok(result, traceId));
    }

    /** Returns R07 access to a previously recorded batch job outcome. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobResponse> getBulkJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        requireAnyRole(user, "R07");
        return ApiResponse.ok(service.getBulkJob(jobId, user), requestId(requestId));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private void requireAnyRole(CurrentUser user, String... roles) {
        if (user.roles() == null || user.roles().stream().noneMatch(role -> List.of(roles).contains(role))) {
            throw new ForbiddenException();
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 0) {
            throw new BusinessValidationException(
                    "페이지 번호가 올바르지 않습니다.",
                    List.of(new ValidationError("page", "0 이상을 입력하세요.")));
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String requestId(String supplied) {
        return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied.trim();
    }
}
