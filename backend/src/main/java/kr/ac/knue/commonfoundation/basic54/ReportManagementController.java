package kr.ac.knue.commonfoundation.basic54;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportManagementController {
    private final ReportManagementService service;

    public ReportManagementController(ReportManagementService service) {
        this.service = service;
    }

    @GetMapping("/api/business/reports")
    public ApiResponse<ReportSearchResponse> listReports(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(required = false) String reportId, @RequestParam(required = false) String businessCategory,
                                                         @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "false") boolean includeInactive,
                                                         @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateSize(size);
        return ApiResponse.ok(service.listReports(user(request), new ReportSearchCriteria(page, size, reportId, businessCategory, activeYn, keyword, includeInactive)), rid(requestId));
    }

    @PostMapping("/api/business/reports/save")
    public ApiResponse<ReportRow> saveReport(@Valid @RequestBody ReportSaveRequest body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveReport(body, user(request), rid), rid);
    }

    @PostMapping("/api/business/reports/{reportId}/outputs")
    public ApiResponse<ReportOutputResponse> createReportOutput(@PathVariable String reportId, @Valid @RequestBody ReportOutputRequest body,
                                                                @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                                                HttpServletRequest request) {
        String rid = rid(requestId);
        ReportOutputRequest scoped = new ReportOutputRequest(reportId, body.outputFormat(), body.outputBaseDate(), body.targetPersonIds(), body.targetSummary());
        return ApiResponse.ok(service.createReportOutput(scoped, user(request), rid), rid);
    }

    @GetMapping("/api/business/report-form-versions")
    public ApiResponse<ReportFormVersionSearchResponse> listReportFormVersions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                                               @RequestParam(required = false) String reportId, @RequestParam(required = false) LocalDate baseDate,
                                                                               @RequestParam(required = false) String keyword,
                                                                               @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateSize(size);
        return ApiResponse.ok(service.listReportFormVersions(user(request), new ReportOperationCriteria(page, size, reportId, null, null, null, null, null, null, null, baseDate, keyword, null, null)), rid(requestId));
    }

    @GetMapping("/api/business/report-form-versions/current")
    public ApiResponse<ReportFormVersionRow> getApplicableReportFormVersion(@RequestParam String reportId, @RequestParam(required = false) LocalDate baseDate,
                                                                            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.applicableFormVersion(user(request), reportId, baseDate), rid);
    }

    @PostMapping("/api/business/report-form-versions/save")
    public ApiResponse<ReportFormVersionRow> saveReportFormVersion(@Valid @RequestBody ReportFormVersionSaveRequest body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveReportFormVersion(body, user(request), rid), rid);
    }

    @GetMapping("/api/business/report-permissions")
    public ApiResponse<ReportPermissionSearchResponse> listReportPermissions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                                             @RequestParam(required = false) String reportId, @RequestParam(required = false) String granteeType,
                                                                             @RequestParam(required = false) String granteeId,
                                                                             @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateSize(size);
        return ApiResponse.ok(service.listReportPermissions(user(request), new ReportOperationCriteria(page, size, reportId, granteeType, granteeId, null, null, null, null, null, null, null, null, null)), rid(requestId));
    }

    @PostMapping("/api/business/report-permissions/save")
    public ApiResponse<ReportPermissionSearchResponse> saveReportPermissions(@Valid @RequestBody BulkReportPermissionsSaveRequest body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveReportPermissions(body, user(request), rid), rid);
    }

    @GetMapping("/api/business/report-print-histories")
    public ApiResponse<ReportPrintHistorySearchResponse> listReportPrintHistories(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                                                  @RequestParam(required = false) String reportId, @RequestParam(required = false) String requesterId,
                                                                                  @RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate,
                                                                                  @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateSize(size);
        return ApiResponse.ok(service.listPrintHistories(user(request), new ReportOperationCriteria(page, size, reportId, null, null, requesterId, fromDate, toDate, null, null, null, null, null, null)), rid(requestId));
    }

    @GetMapping("/api/business/bulk-report-jobs")
    public ApiResponse<BulkReportJobSearchResponse> listBulkReportJobs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(required = false) String reportId, @RequestParam(required = false) String status,
                                                                       @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateSize(size);
        return ApiResponse.ok(service.listBulkReportJobs(user(request), new ReportOperationCriteria(page, size, reportId, null, null, null, null, null, status, null, null, null, null, null)), rid(requestId));
    }

    @PostMapping("/api/business/bulk-report-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<BulkReportJobRow> createBulkReportJob(@Valid @RequestBody BulkReportJobCreateRequest body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.createBulkReportJob(body, user(request), rid), rid);
    }

    @GetMapping("/api/business/bulk-report-jobs/{jobId}")
    public ApiResponse<BulkReportJobRow> getBulkReportJob(@PathVariable Long jobId, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        return ApiResponse.ok(service.getBulkReportJob(user(request), jobId), rid(requestId));
    }

    @GetMapping("/api/business/bulk-report-jobs/{jobId}/result")
    public ApiResponse<BulkReportJobResultResponse> downloadBulkReportJobResult(@PathVariable Long jobId, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        return ApiResponse.ok(service.getBulkReportJobResult(user(request), jobId), rid(requestId));
    }

    @GetMapping("/api/business/bulk-report-jobs/targets")
    public ApiResponse<BulkReportTargetSearchResponse> listBulkReportTargets(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                                                             @RequestParam(required = false) String reportId,
                                                                             @RequestParam(required = false) String evaluationYear,
                                                                             @RequestParam(required = false) String organizationCode,
                                                                             @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateSize(size);
        return ApiResponse.ok(service.listBulkReportTargets(user(request), new ReportOperationCriteria(page, size, reportId, null, null, null, null, null, null, null, null, null, evaluationYear, organizationCode)), rid(requestId));
    }

    private CurrentUser user(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }

    private String rid(String requestId) { return requestId != null && !requestId.isBlank() ? requestId.trim() : UUID.randomUUID().toString(); }
    private void validateSize(int size) { if (size != 20 && size != 50 && size != 100) throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", java.util.List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요."))); }
}

