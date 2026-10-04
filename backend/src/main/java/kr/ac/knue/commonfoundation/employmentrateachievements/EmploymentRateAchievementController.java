package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owns the approved employment-rate achievement HTTP operations, enforcing
 * role boundaries before delegating data-scope, policy, and persistence work to the service.
 */
@RestController
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists the caller-visible employment-rate achievement rows. */
    @GetMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementViews.SearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePage(page, pageSize);
        return ApiResponse.ok(service.list(page, pageSize, requireReader(request)), traceId(requestId));
    }

    /** Creates one caller-owned employment-rate DRAFT record. */
    @PostMapping("/api/business/employment-rate-achievements")
    public ApiResponse<EmploymentRateAchievementViews.Achievement> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, requireWriter(request), traceId), traceId);
    }

    /** Returns the selected employment-rate achievement record. */
    @GetMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementViews.Achievement> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireReader(request)), traceId(requestId));
    }

    /** Updates a non-finalized employment-rate achievement owned by the R01 caller. */
    @PutMapping("/api/business/employment-rate-achievements/{achievementId}")
    public ApiResponse<EmploymentRateAchievementViews.Achievement> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, requireWriter(request), traceId), traceId);
    }

    /** Exports the caller-visible current page as the OpenAPI-required XLSX binary. */
    @GetMapping("/api/business/employment-rate-achievements/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        validatePage(page, pageSize);
        EmploymentRateAchievementViews.SearchResponse response = service.download(
                page,
                pageSize,
                requireDownloadRole(request));
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("관리번호", "관리항목코드", "업적발생일", "실적명", "상태"));
        for (EmploymentRateAchievementViews.Achievement row : response.achievements()) {
            rows.add(List.of(
                    empty(row.managementNo()),
                    empty(row.managementItemCode()),
                    row.achievementDate() == null ? "" : row.achievementDate().toString(),
                    empty(row.achievementName()),
                    empty(row.achievementStatus())));
        }
        try {
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("employment-rate-achievements.xlsx")
                                    .build()
                                    .toString())
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(XlsxWorkbook.write(rows));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("취업률 실적 Excel 파일을 생성하지 못했습니다.", exception);
        }
    }

    /** Validates and atomically commits an R07 employment-rate upload or preserves only its error history. */
    @PostMapping(
            value = "/api/business/employment-rate-achievements/excel-uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EmploymentRateAchievementViews.UploadResult> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.upload(file, requireExcelRole(request), traceId), traceId);
    }

    /**
     * Receives an R07 bulk request. The service deliberately returns 409 until
     * OQ-83-01 approves target preview and action semantics.
     */
    @PostMapping("/api/business/employment-rate-achievements/bulk-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<EmploymentRateAchievementViews.BulkJob> requestBulkJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.requestBulkJob(body, requireExcelRole(request), traceId), traceId);
    }

    /** Returns a persisted R07 bulk-job status and aggregate result. */
    @GetMapping("/api/business/employment-rate-achievements/bulk-jobs/{jobId}")
    public ApiResponse<EmploymentRateAchievementViews.BulkJob> getBulkJob(
            @PathVariable String jobId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.getBulkJob(jobId, requireExcelRole(request)), traceId(requestId));
    }

    private CurrentUser requireReader(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R07").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriter(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireDownloadRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R07").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireExcelRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R07")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }

    private void validatePage(int page, int pageSize) {
        if (page < 0) {
            throw new BusinessValidationException(
                    "페이지 값이 올바르지 않습니다.",
                    List.of(new ValidationError("page", "0 이상이어야 합니다.")));
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }
}
