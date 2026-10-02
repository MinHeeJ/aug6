package kr.ac.knue.commonfoundation.basic83;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
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
import org.springframework.web.bind.annotation.RestController;

/** Exposes the BASIC-83 취업률 list, detail, create, and update HTTP contract. */
@RestController
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(
            EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists caller-scoped 취업률 rows with only the approved page sizes. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        String traceId = traceId(requestId);
        return ApiResponse.ok(
                service.list(
                        new EmploymentRateAchievementSearchCriteria(
                                page,
                                pageSize,
                                managementNo,
                                teacherName,
                                managementItemCode,
                                certificationStatus),
                        requireReadUser(request)),
                traceId);
    }

    /** Returns a caller-authorized detail row. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.get(achievementId, requireReadUser(request)), traceId);
    }

    /** Creates an R01-owned DRAFT row through the guarded service transaction. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementSaveResult> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, requireWriteUser(request), traceId), traceId);
    }

    /** Updates an R01-owned row while preserving service-level period and finalization protections. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementSaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(
                service.update(achievementId, body, requireWriteUser(request), traceId),
                traceId);
    }

    /** Downloads an authorized employment-rate workbook in the approved Office Open XML format. */
    @GetMapping(
            value = "/api/business/employment-rate-achievements/download",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        byte[] workbook = service.download(
                new EmploymentRateAchievementSearchCriteria(
                        page,
                        pageSize,
                        null,
                        null,
                        null,
                        null),
                requireDownloadUser(request));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=employment-rate-achievements.xlsx")
                .body(workbook);
    }

    /** Returns the persisted per-target outcome for an R07 bulk job. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateBulkJobResponse> getBulkJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.getBulkJob(jobId, requireExcelOperator(request)), traceId);
    }

    /**
     * Rejects a bulk request before persistence while the approved target-preview and
     * confirmation policy remains unresolved; this preserves the required no-side-effect 409 contract.
     */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    public ApiResponse<Void> createBulkJob(
            @RequestBody EmploymentRateBulkJobRequest body,
            HttpServletRequest request) {
        requireExcelOperator(request);
        throw new ConflictException("취업률 일괄 처리 정책이 확정되지 않아 작업을 접수할 수 없습니다.");
    }

    /** Rejects R07 Excel upload until the shared template contract supplies an approved field mapping. */
    @PostMapping("/api/business/employment-rate-achievements/excel-uploads")
    public ApiResponse<Void> uploadExcel(HttpServletRequest request) {
        requireExcelOperator(request);
        throw new ConflictException("취업률 Excel 양식과 검증 규칙이 확정되지 않아 반영할 수 없습니다.");
    }

    private CurrentUser requireExcelOperator(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R07")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireDownloadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(
                        role -> List.of("R01", "R02", "R04", "R07", "R09").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireReadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriteUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
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

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
