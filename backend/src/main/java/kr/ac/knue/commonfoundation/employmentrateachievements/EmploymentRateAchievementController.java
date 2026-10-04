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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP boundary for employment-rate records, shared Excel validation, and R07
 * batch-result lookups. It keeps authorization at the transport boundary and
 * delegates persistence and transaction behavior to the service.
 */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists caller-visible records using optional dynamic filters and approved page sizes. */
    @GetMapping
    public ApiResponse<EmploymentRateAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        return ApiResponse.ok(
                service.list(
                        new EmploymentRateAchievementSearchCriteria(
                                page, pageSize, managementNo, managementItemCode, achievementStatus),
                        currentUser(request, "R01", "R02", "R04")),
                effectiveRequestId(requestId));
    }

    /** Gets a single record only after the service validates the caller's data scope. */
    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.get(achievementId, currentUser(request, "R01", "R02", "R04")),
                effectiveRequestId(requestId));
    }

    /** Creates one R01-owned employment-rate record. */
    @PostMapping
    public ApiResponse<EmploymentRateAchievementService.EmploymentRateAchievementSaveResult> create(
            @Valid @RequestBody EmploymentRateAchievementSaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, currentUser(request, "R01"), traceId), traceId);
    }

    /** Updates the path-selected record; request bodies do not carry mutable identifiers. */
    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateAchievementService.EmploymentRateAchievementSaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementSaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, currentUser(request, "R01"), traceId), traceId);
    }

    /** Exports a caller-scoped CSV payload with an Excel-compatible content disposition. */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        EmploymentRateAchievementSearchResponse result = service.list(
                new EmploymentRateAchievementSearchCriteria(page, pageSize, null, null, null),
                currentUser(request, "R01", "R02", "R04", "R07"));
        StringBuilder body = new StringBuilder("관리번호,관리항목,업적발생일,실적명,상태\n");
        result.achievements().forEach(row -> body.append(row.managementNo()).append(',')
                .append(row.managementItemCode()).append(',')
                .append(row.achievementDate()).append(',')
                .append(csv(row.achievementName())).append(',')
                .append(row.achievementStatus()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employment-rate-achievements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Validates an R07 file through the existing Excel service before any business commit. */
    @PostMapping(value = "/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.upload(file, currentUser(request, "R07")), effectiveRequestId(requestId));
    }

    /** Rejects policy-unapproved execution requests without creating a batch-job row. */
    @PostMapping("/bulk-jobs")
    public ResponseEntity<ApiResponse<EmploymentRateBulkJobRow>> requestBatchJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(
                service.requestBatchJob(body, currentUser(request, "R07")),
                effectiveRequestId(requestId)));
    }

    /** Retrieves the immutable R07 batch outcome including unprocessed target reasons. */
    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobRow> getBatchJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.getBatchJob(jobId, currentUser(request, "R07")), effectiveRequestId(requestId));
    }

    private CurrentUser currentUser(HttpServletRequest request, String... roles) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(List.of(roles)::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
