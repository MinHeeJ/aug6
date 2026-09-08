package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportManagementService {
    private static final Set<String> ADMIN_ROLES = Set.of("R04", "R09");
    private static final Set<String> REPORT_ROLES = Set.of("R01", "R03", "R04", "R09");
    private static final Set<String> PRINT_HISTORY_ROLES = Set.of("R04", "R08", "R09");
    private static final Set<String> BULK_REPORT_ROLES = Set.of("R03", "R04", "R09");
    private static final Set<String> YN = Set.of("Y", "N");
    private final ReportManagementMapper mapper;
    private final ReportPolicyProperties reportPolicies;

    public ReportManagementService(ReportManagementMapper mapper, ReportPolicyProperties reportPolicies) {
        this.mapper = mapper;
        this.reportPolicies = reportPolicies;
    }

    public ReportPolicyProperties reportPolicies() {
        return reportPolicies;
    }

    @Transactional(readOnly = true)
    public ReportSearchResponse listReports(CurrentUser user, ReportSearchCriteria criteria) {
        requireAnyRole(user, ADMIN_ROLES);
        ReportSearchCriteria scoped = new ReportSearchCriteria(criteria.page(), criteria.pageSize(), norm(criteria.reportId()), norm(criteria.businessCategory()), norm(criteria.activeYn()), trim(criteria.keyword()), criteria.includeInactive());
        return new ReportSearchResponse(mapper.listReports(scoped), Math.max(criteria.page(), 0), scoped.safeSize(), mapper.countReports(scoped));
    }

    @Transactional
    public ReportRow saveReport(ReportSaveRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, ADMIN_ROLES);
        validateReport(request);
        ReportRow before = mapper.findReportById(norm(request.reportId()), true);
        ReportRow after = before == null ? mapper.insertReport(request, user.userId()) : mapper.updateReport(request, user.userId());
        record("reports", after.reportId(), before, after, user, request.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public ReportFormVersionSearchResponse listReportFormVersions(CurrentUser user, ReportOperationCriteria criteria) {
        requireAnyRole(user, ADMIN_ROLES);
        return new ReportFormVersionSearchResponse(mapper.listReportFormVersions(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countReportFormVersions(criteria));
    }

    @Transactional(readOnly = true)
    public ReportFormVersionRow applicableFormVersion(CurrentUser user, String reportId, LocalDate baseDate) {
        requireAnyRole(user, REPORT_ROLES);
        if (!hasText(reportId)) throw validation("reportId", "보고서 ID를 선택하세요.");
        LocalDate effectiveBaseDate = baseDate == null ? LocalDate.now() : baseDate;
        ReportFormVersionRow row = mapper.findApplicableFormVersion(norm(reportId), effectiveBaseDate);
        if (row == null) throw new NotFoundException("기준일에 적용되는 보고서 양식 버전을 찾을 수 없습니다.");
        return row;
    }

    @Transactional
    public ReportFormVersionRow saveReportFormVersion(ReportFormVersionSaveRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, ADMIN_ROLES);
        validateForm(request);
        ensureReportExists(request.reportId());
        ReportFormVersionRow before = request.formVersionId() == null ? null : mapper.findReportFormVersionById(request.formVersionId());
        if (request.formVersionId() != null && before == null) throw new NotFoundException("보고서 양식 버전을 찾을 수 없습니다.");
        ReportFormVersionSaveRequest effectiveRequest = new ReportFormVersionSaveRequest(request.formVersionId(), request.reportId(), request.versionName(), request.effectiveDate(), request.formFileRef(), hasText(request.currentYn()) ? request.currentYn() : "Y", request.changeReason());
        ReportFormVersionRow after = before == null ? mapper.insertReportFormVersion(effectiveRequest, user.userId()) : mapper.updateReportFormVersion(effectiveRequest, user.userId());
        record("report_form_versions", String.valueOf(after.formVersionId()), before, after, user, effectiveRequest.changeReason(), requestId);
        return after;
    }

    @Transactional
    public ReportPermissionSearchResponse saveReportPermissions(BulkReportPermissionsSaveRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, ADMIN_ROLES);
        for (ReportPermissionSaveRequest permission : request.permissions()) {
            String reason = hasText(permission.changeReason()) ? permission.changeReason() : request.changeReason();
            ReportPermissionSaveRequest effectivePermission = new ReportPermissionSaveRequest(permission.permissionId(), permission.granteeType(), permission.granteeId(), permission.reportId(), permission.allowViewYn(), permission.allowPreviewYn(), permission.allowPrintYn(), permission.allowPdfYn(), permission.allowExcelYn(), permission.dataScope(), permission.activeYn(), reason);
            saveReportPermission(effectivePermission, user, requestId);
        }
        return listReportPermissions(user, new ReportOperationCriteria(0, 100, null, null, null, null, null, null, null, null, null, null, null, null));
    }

    @Transactional(readOnly = true)
    public ReportPermissionSearchResponse listReportPermissions(CurrentUser user, ReportOperationCriteria criteria) {
        requireAnyRole(user, ADMIN_ROLES);
        return new ReportPermissionSearchResponse(mapper.listReportPermissions(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countReportPermissions(criteria));
    }

    @Transactional
    public ReportPermissionRow saveReportPermission(ReportPermissionSaveRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, ADMIN_ROLES);
        validatePermission(request);
        ensureReportExists(request.reportId());
        ReportPermissionRow before = request.permissionId() == null ? null : mapper.findReportPermissionById(request.permissionId());
        if (request.permissionId() != null && before == null) throw new NotFoundException("보고서 권한을 찾을 수 없습니다.");
        ReportPermissionRow after = before == null ? mapper.insertReportPermission(request, user.userId()) : mapper.updateReportPermission(request, user.userId());
        record("report_permissions", String.valueOf(after.permissionId()), before, after, user, request.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public ReportPermissionDecision checkPermission(CurrentUser user, String reportId, String actionType, String outputFormat) {
        requireAnyRole(user, REPORT_ROLES);
        ensureReportExists(reportId);
        return decidePermission(user, reportId, actionType, outputFormat);
    }

    @Transactional
    public ReportOutputResponse createReportOutput(ReportOutputRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, REPORT_ROLES);
        validateOutput(request.reportId(), request.outputFormat(), request.targetPersonIds(), request.targetSummary());
        ReportRow report = requireActiveReport(request.reportId());
        String reportId = norm(request.reportId());
        String outputFormat = norm(request.outputFormat());
        String targetSummary = request.targetSummary().trim();
        ReportPermissionDecision decision = decidePermission(user, reportId, "PRINT", outputFormat);
        if (!decision.allowed()) {
            recordFailedPrintHistoryIfEnabled(reportId, user.userId(), targetSummary, outputFormat, "FORBIDDEN", requestId);
            throw new ForbiddenException();
        }
        LocalDate outputBaseDate = request.outputBaseDate() == null ? LocalDate.now() : request.outputBaseDate();
        ReportFormVersionRow form = mapper.findApplicableFormVersion(reportId, outputBaseDate);
        if (form == null) throw new NotFoundException("기준일에 적용되는 보고서 양식 버전을 찾을 수 없습니다.");
        String fileRef = outputFileRef(reportId, form.versionName(), outputFormat, requestId);
        int outputCount = request.targetPersonIds().size();
        mapper.insertReportPrintHistory(reportId, user.userId(), targetSummary, outputFormat, outputCount, "SUCCESS", fileRef, requestId);
        return new ReportOutputResponse(report.reportId(), report.reportName(), report.datasetCode(), outputFormat, outputBaseDate, form.formVersionId(), form.versionName(), form.formFileRef(), outputCount, fileRef, requestId);
    }

    @Transactional(readOnly = true)
    public ReportPrintHistorySearchResponse listPrintHistories(CurrentUser user, ReportOperationCriteria criteria) {
        requireAnyRole(user, PRINT_HISTORY_ROLES);
        return new ReportPrintHistorySearchResponse(mapper.listReportPrintHistories(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countReportPrintHistories(criteria));
    }

    @Transactional
    public void recordPrintHistory(String reportId, CurrentUser user, String targetSummary, String outputFormat, int outputCount, String resultCode, String fileRef, String requestId) {
        requireAnyRole(user, REPORT_ROLES);
        mapper.insertReportPrintHistory(norm(reportId), user.userId(), targetSummary, norm(outputFormat), outputCount, norm(resultCode), fileRef, requestId);
    }

    @Transactional(readOnly = true)
    public BulkReportJobSearchResponse listBulkReportJobs(CurrentUser user, ReportOperationCriteria criteria) {
        requireAnyRole(user, BULK_REPORT_ROLES);
        return new BulkReportJobSearchResponse(mapper.listBulkReportJobs(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countBulkReportJobs(criteria));
    }

    @Transactional(readOnly = true)
    public BulkReportJobRow getBulkReportJob(CurrentUser user, Long jobId) {
        requireAnyRole(user, BULK_REPORT_ROLES);
        BulkReportJobRow row = mapper.findBulkReportJobById(jobId);
        if (row == null) throw new NotFoundException("대량 출력 작업을 찾을 수 없습니다.");
        return row;
    }

    @Transactional(readOnly = true)
    public BulkReportJobResultResponse getBulkReportJobResult(CurrentUser user, Long jobId) {
        BulkReportJobRow row = getBulkReportJob(user, jobId);
        return BulkReportJobResultResponse.from(row, mapper.listBulkReportJobFailures(jobId));
    }

    @Transactional
    public BulkReportJobRow createBulkReportJob(BulkReportJobCreateRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, BULK_REPORT_ROLES);
        validateOutput(request.reportId(), request.outputFormat() == null ? "PDF" : request.outputFormat(), request.targetPersonIds(), "대량 출력 대상");
        String reportId = norm(request.reportId());
        requireActiveReport(reportId);
        String outputFormat = norm(request.outputFormat() == null ? "PDF" : request.outputFormat());
        ReportPermissionDecision decision = decidePermission(user, reportId, "PRINT", outputFormat);
        if (!decision.allowed()) {
            recordFailedPrintHistoryIfEnabled(reportId, user.userId(), "대량 출력 권한 차단", outputFormat, "FORBIDDEN", requestId);
            throw new ForbiddenException();
        }
        LocalDate outputBaseDate = request.outputBaseDate() == null ? LocalDate.now() : request.outputBaseDate();
        ReportFormVersionRow form = mapper.findApplicableFormVersion(reportId, outputBaseDate);
        if (form == null) throw new NotFoundException("기준일에 적용되는 보고서 양식 버전을 찾을 수 없습니다.");
        if (mapper.countRunningBulkReportJobs(reportId, request.targetHash()) > 0) throw new ConflictException("JOB_ALREADY_RUNNING: 진행 중인 동일 대량 출력 작업이 있습니다.");
        BulkReportJobRow row = mapper.insertBulkReportJob(request, user.userId(), requestId, request.targetPersonIds().size());
        request.targetPersonIds().forEach(targetId -> mapper.insertBulkReportJobTarget(row.jobId(), targetId));
        mapper.insertReportPrintHistory(reportId, user.userId(), "대량 출력 준비 대상 " + request.targetPersonIds().size() + "건", outputFormat,
                request.targetPersonIds().size(), "QUEUED", null, requestId);
        return row;
    }

    @Transactional
    public BulkReportJobRow completeBulkReportJob(Long jobId, String resultFileRef) {
        BulkReportJobRow row = mapper.findBulkReportJobById(jobId);
        if (row == null) throw new NotFoundException("대량 출력 작업을 찾을 수 없습니다.");
        return mapper.completeBulkReportJob(jobId, resultFileRef);
    }

    @Transactional(readOnly = true)
    public BulkReportTargetSearchResponse listBulkReportTargets(CurrentUser user, ReportOperationCriteria criteria) {
        requireAnyRole(user, BULK_REPORT_ROLES);
        return new BulkReportTargetSearchResponse(mapper.listBulkReportTargets(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countBulkReportTargets(criteria));
    }

    private boolean allows(ReportPermissionRow row, String actionType, String outputFormat) {
        String action = norm(actionType);
        String format = norm(outputFormat);
        boolean actionAllowed = switch (action == null ? "VIEW" : action) {
            case "PREVIEW" -> "Y".equals(row.allowPreviewYn());
            case "PRINT" -> "Y".equals(row.allowPrintYn());
            default -> "Y".equals(row.allowViewYn());
        };
        boolean formatAllowed = format == null || switch (format) {
            case "PDF" -> "Y".equals(row.allowPdfYn());
            case "EXCEL" -> "Y".equals(row.allowExcelYn());
            default -> false;
        };
        return actionAllowed && formatAllowed;
    }

    private ReportPermissionDecision decidePermission(CurrentUser user, String reportId, String actionType, String outputFormat) {
        List<ReportPermissionRow> rows = mapper.findActivePermissionsForReport(norm(reportId), user.roles(), user.userId());
        if (reportPolicies.permissionMergeStrategy() == ReportPolicyProperties.PermissionMergeStrategy.PRIORITY_FIRST_MATCH) {
            return rows.isEmpty() || !allows(rows.get(0), actionType, outputFormat)
                    ? ReportPermissionDecision.deny("REPORT_PERMISSION_DENIED")
                    : ReportPermissionDecision.allow();
        }
        for (ReportPermissionRow row : rows) {
            if (allows(row, actionType, outputFormat)) return ReportPermissionDecision.allow();
        }
        return ReportPermissionDecision.deny("REPORT_PERMISSION_DENIED");
    }

    private void recordFailedPrintHistoryIfEnabled(String reportId, Long userId, String targetSummary, String outputFormat, String resultCode, String requestId) {
        if (reportPolicies.recordFailedPrintHistory()) {
            mapper.insertReportPrintHistory(reportId, userId, targetSummary, outputFormat, 0, resultCode, null, requestId);
        }
    }

    private ReportRow requireActiveReport(String reportId) {
        ReportRow row = mapper.findReportById(norm(reportId), false);
        if (row == null) throw new NotFoundException("사용 중인 보고서를 찾을 수 없습니다.");
        return row;
    }

    private String outputFileRef(String reportId, String versionName, String outputFormat, String requestId) {
        String extension = "EXCEL".equals(outputFormat) ? "xlsx" : "pdf";
        String safeRequestId = requestId == null ? "NO-REQUEST-ID" : requestId.replaceAll("[^A-Za-z0-9_-]", "-");
        return "reports/generated/" + reportId + "-" + versionName + "-" + safeRequestId + "." + extension;
    }

    private void ensureReportExists(String reportId) {
        if (mapper.findReportById(norm(reportId), false) == null) throw new NotFoundException("사용 중인 보고서를 찾을 수 없습니다.");
    }

    private void validateReport(ReportSaveRequest r) {
        List<ValidationError> fields = new ArrayList<>();
        required(fields, "reportId", r.reportId(), "보고서 ID를 입력하세요.");
        required(fields, "reportName", r.reportName(), "보고서명을 입력하세요.");
        required(fields, "businessCategory", r.businessCategory(), "업무구분을 입력하세요.");
        required(fields, "templateFileRef", r.templateFileRef(), "템플릿 파일 참조를 입력하세요.");
        required(fields, "datasetCode", r.datasetCode(), "데이터셋 코드를 입력하세요.");
        required(fields, "changeReason", r.changeReason(), "변경 사유를 입력하세요.");
        flag(fields, "activeYn", r.activeYn());
        if (!fields.isEmpty()) throw new BusinessValidationException("보고서 저장 요청이 올바르지 않습니다.", fields);
    }

    private void validateOutput(String reportId, String outputFormat, List<Long> targetPersonIds, String targetSummary) {
        List<ValidationError> fields = new ArrayList<>();
        required(fields, "reportId", reportId, "보고서 ID를 선택하세요.");
        required(fields, "outputFormat", outputFormat, "출력 형식을 선택하세요.");
        required(fields, "targetSummary", targetSummary, "대상 범위 설명을 입력하세요.");
        if (!Set.of("PDF", "EXCEL").contains(norm(outputFormat))) fields.add(new ValidationError("outputFormat", "PDF 또는 EXCEL만 허용됩니다."));
        if (targetPersonIds == null || targetPersonIds.isEmpty()) fields.add(new ValidationError("targetPersonIds", "출력 대상을 선택하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("보고서 출력 요청이 올바르지 않습니다.", fields);
    }

    private void validateForm(ReportFormVersionSaveRequest r) {
        List<ValidationError> fields = new ArrayList<>();
        if (hasText(r.currentYn())) flag(fields, "currentYn", r.currentYn());
        if (!fields.isEmpty()) throw new BusinessValidationException("보고서 양식 버전 저장 요청이 올바르지 않습니다.", fields);
    }

    private void validatePermission(ReportPermissionSaveRequest r) {
        List<ValidationError> fields = new ArrayList<>();
        for (String field : List.of("allowViewYn", "allowPreviewYn", "allowPrintYn", "allowPdfYn", "allowExcelYn", "activeYn")) {
            String value = switch (field) {
                case "allowViewYn" -> r.allowViewYn(); case "allowPreviewYn" -> r.allowPreviewYn(); case "allowPrintYn" -> r.allowPrintYn(); case "allowPdfYn" -> r.allowPdfYn(); case "allowExcelYn" -> r.allowExcelYn(); default -> r.activeYn();
            };
            flag(fields, field, value);
        }
        if (!Set.of("ROLE", "ORG", "USER").contains(norm(r.granteeType()))) fields.add(new ValidationError("granteeType", "ROLE, ORG, USER 중 하나를 선택하세요."));
        if ("N".equals(norm(r.allowViewYn())) && "N".equals(norm(r.allowPreviewYn())) && "N".equals(norm(r.allowPrintYn()))
                && "N".equals(norm(r.allowPdfYn())) && "N".equals(norm(r.allowExcelYn()))) {
            fields.add(new ValidationError("allowedActions", "하나 이상의 허용행위를 선택하세요."));
        }
        required(fields, "changeReason", r.changeReason(), "변경 사유를 입력하세요.");
        if (!fields.isEmpty()) throw new BusinessValidationException("보고서 권한 저장 요청이 올바르지 않습니다.", fields);
    }

    private void flag(List<ValidationError> fields, String field, String value) { if (!YN.contains(norm(value))) fields.add(new ValidationError(field, "Y 또는 N을 선택하세요.")); }
    private void required(List<ValidationError> fields, String field, String value, String message) { if (!hasText(value)) fields.add(new ValidationError(field, message)); }
    private void record(String target, String key, Object before, Object after, CurrentUser user, String reason, String requestId) { mapper.insertChangeHistory(target, key, before == null ? "CREATE" : "UPDATE", "setting", before == null ? null : before.toString(), after.toString(), user.userId(), reason, requestId); }
    private void requireAnyRole(CurrentUser user, Set<String> roles) { if (user == null) throw new UnauthenticatedException(); if (user.roles().stream().noneMatch(roles::contains)) throw new ForbiddenException(); }
    private BusinessValidationException validation(String field, String message) { return new BusinessValidationException(message, List.of(new ValidationError(field, message))); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String norm(String value) { String trimmed = trim(value); return trimmed == null ? null : trimmed.toUpperCase(); }
}
