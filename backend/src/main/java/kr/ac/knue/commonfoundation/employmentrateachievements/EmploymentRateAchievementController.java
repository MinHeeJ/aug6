package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
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

/** HTTP boundary for individual employment-rate achievements and R07-only batch actions. */
@RestController
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) { this.service = service; }

    /** Lists data-scoped individual achievements for R01/R02/R04. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = current(request);
        if (user.roles() == null
                || user.roles().stream()
                        .noneMatch(role -> List.of("R01", "R02", "R04", "R07").contains(role))) {
            throw new ForbiddenException();
        }
        return ApiResponse.ok(service.list(page, pageSize, user), trace(requestId));
    }

    /** Creates an own R01 source record through the guarded transaction service. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementRow> create(@Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String trace = trace(requestId);
        return ApiResponse.ok(service.create(body, current(request), trace), trace);
    }

    /** Retrieves an authorized detailed source record. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> get(@PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, current(request)), trace(requestId));
    }

    /** Updates the selected R01-owned record rather than accepting an identifier in the payload. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> update(@PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String trace = trace(requestId);
        return ApiResponse.ok(service.update(achievementId, body, current(request), trace), trace);
    }

    /** Downloads a UTF-8 spreadsheet-compatible CSV for the current authorized result set. */
    @GetMapping("/api/business/employment-rate-achievements/download")
    public ResponseEntity<byte[]> download(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = current(request);
        if (user.roles() == null ||
        user.roles()
        .stream()
        .noneMatch(role -> List.of("R01",
        "R02",
        "R04",
        "R07")
        .contains(role))) throw new ForbiddenException();
        EmploymentRateAchievementSearchResponse result = service.list(page, pageSize, user);
        StringBuilder csv = new StringBuilder(
                "managementNo,managementItemCode,achievementDate,achievementName,certificationStatus\n");
        for (EmploymentRateAchievementRow row : result.achievements()) csv.append(row.managementNo())
        .append(',')
        .append(row.managementItemCode())
        .append(',')
        .append(row.achievementDate())
        .append(',')
        .append(row.achievementName())
        .append(',')
        .append(row.certificationStatus())
        .append('\n');
        return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment()
        .filename("employment-rate-achievements.csv",
        StandardCharsets.UTF_8)
        .build()
        .toString())
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv.toString()
        .getBytes(StandardCharsets.UTF_8));
    }

    /** Validates an R07 upload and returns retained diagnostics with zero domain persistence on error. */
    @PostMapping(value = "/api/business/employment-rate-achievements/excel-uploads",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateExcelUploadResult> upload(@RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = current(request);
        if (user.roles() == null || !user.roles().contains("R07")) {
            throw new ForbiddenException();
        }
        return ApiResponse.ok(service.upload(file, user), trace(requestId));
    }

    /** Deliberately returns the unresolved-policy conflict until OQ-83-01 is approved. */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    public ResponseEntity<ApiResponse<Void>> bulk(@Valid @RequestBody EmploymentRateBulkJobRequest body,
        HttpServletRequest request) {
        service.createBulkJob(body, current(request));
        return ResponseEntity.accepted().body(ApiResponse.ok(null));
    }

    /** Returns R07's own persisted bulk job result. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobResult> bulkResult(@PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        return ApiResponse.ok(service.getBulkJob(jobId, current(request)), trace(requestId));
    }

    private CurrentUser current(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser current)) {
            throw new UnauthenticatedException();
        }
        return current;
    }
    private String trace(String requestId) { return requestId == null ||
        requestId.isBlank() ? UUID.randomUUID()
        .toString() : requestId.trim(); }
    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요."))
            );
        }
    }
}
