package kr.ac.knue.commonfoundation.basic60;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
public class Basic60Controller {
    private final Basic60Service service;

    public Basic60Controller(Basic60Service service) {
        this.service = service;
    }

    @GetMapping("/api/admin/evaluation-element-management-item-settings")
    public ApiResponse<OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse> listEvaluationElementManagementItemSettings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId, @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String areaCode, @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireSettingsAdmin(servletRequest);
        validatePageSize(pageSize);
        return ApiResponse.ok(service.listElementSettings(new OperationalSettingSearchCriteria(page, pageSize, ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode, null, managementItemCode, null, null, null, activeYn, keyword)), effectiveRequestId(requestId));
    }

    /** Downloads the filtered evaluation-element settings visible to an authorized settings administrator. */
    @GetMapping("/api/admin/evaluation-element-management-item-settings/download")
    public ResponseEntity<byte[]> downloadEvaluationElementManagementItemSettings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId, @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String areaCode, @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireSettingsAdmin(servletRequest);
        validatePageSize(pageSize);
        String traceId = effectiveRequestId(requestId);
        return excelAttachment(service.downloadElementSettings(new OperationalSettingSearchCriteria(page, pageSize, ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode, null, managementItemCode, null, null, null, activeYn, keyword)), "evaluation-element-management-item-settings.xlsx", traceId);
    }

    @PostMapping("/api/admin/evaluation-element-management-item-settings/save")
    public ApiResponse<OperationalSettingRow> saveEvaluationElementManagementItemSetting(
            @Valid @RequestBody SaveEvaluationElementManagementItemSettingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSettingsAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.saveElementSetting(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    @GetMapping("/api/admin/participation-allocation-rate-settings")
    public ApiResponse<OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse> listParticipationAllocationRateSettings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId, @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String areaCode, @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) Integer researcherCount,
            @RequestParam(required = false) String participationType, @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireSettingsAdmin(servletRequest);
        validatePageSize(pageSize);
        return ApiResponse.ok(service.listParticipationSettings(new OperationalSettingSearchCriteria(page, pageSize, ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode, null, managementItemCode, null, researcherCount, participationType, activeYn, keyword)), effectiveRequestId(requestId));
    }

    /** Downloads the currently filtered participation-allocation matrix for the authorized role scope. */
    @GetMapping("/api/admin/participation-allocation-rate-settings/download")
    public ResponseEntity<byte[]> downloadParticipationAllocationRateSettings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId, @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String areaCode, @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) Integer researcherCount,
            @RequestParam(required = false) String participationType, @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireSettingsAdmin(servletRequest);
        validatePageSize(pageSize);
        String traceId = effectiveRequestId(requestId);
        return excelAttachment(service.downloadParticipationSettings(new OperationalSettingSearchCriteria(page, pageSize, ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode, null, managementItemCode, null, researcherCount, participationType, activeYn, keyword)), "participation-allocation-rate-settings.xlsx", traceId);
    }

    /**
     * Saves every submitted participation matrix cell atomically after applying the shared
     * authorization and request-tracing boundary.
     */
    @PostMapping("/api/admin/participation-allocation-rate-settings/save")
    @Transactional
    public ApiResponse<OperationalSettingRow> saveParticipationAllocationRateSetting(
            @Valid @RequestBody SaveParticipationAllocationRateSettingsBatchRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSettingsAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        OperationalSettingRow lastSaved = null;
        for (SaveParticipationAllocationRateSettingsBatchRequest.Item item : request.items()) {
            lastSaved = service.saveParticipationSetting(
                    item.toSaveRequest(request.ruleVersionId(), request.targetScope(), request.changeReason()),
                    user.userId(), effectiveRequestId);
        }
        return ApiResponse.ok(lastSaved, effectiveRequestId);
    }

    @GetMapping("/api/admin/management-item-evaluation-score-settings")
    public ApiResponse<OperationalSettingResponses.ManagementItemEvaluationScoreSettingSearchResponse> listManagementItemEvaluationScoreSettings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId, @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String areaCode, @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        requireSettingsAdmin(servletRequest);
        validatePageSize(pageSize);
        return ApiResponse.ok(service.listScoreSettings(new OperationalSettingSearchCriteria(page, pageSize, ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode, null, managementItemCode, organizationCode, null, null, activeYn, keyword)), effectiveRequestId(requestId));
    }

    /** Downloads the currently filtered management-item score settings for the authorized role scope. */
    @GetMapping("/api/admin/management-item-evaluation-score-settings/download")
    public ResponseEntity<byte[]> downloadManagementItemEvaluationScoreSettings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId, @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String areaCode, @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        requireSettingsAdmin(servletRequest);
        validatePageSize(pageSize);
        String traceId = effectiveRequestId(requestId);
        return excelAttachment(service.downloadScoreSettings(new OperationalSettingSearchCriteria(page, pageSize, ruleVersionId, targetScope, areaCode, itemCode, evaluationYear, elementCode, null, managementItemCode, organizationCode, null, null, activeYn, keyword)), "management-item-evaluation-score-settings.xlsx", traceId);
    }

    @PostMapping("/api/admin/management-item-evaluation-score-settings/save")
    public ApiResponse<OperationalSettingRow> saveManagementItemEvaluationScoreSetting(
            @Valid @RequestBody SaveManagementItemEvaluationScoreSettingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSettingsAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.saveScoreSetting(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireSettingsAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R09")) return currentUser;
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }


    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", java.util.List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private ResponseEntity<byte[]> excelAttachment(byte[] workbook, String filename, String requestId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("X-Request-Id", requestId)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(workbook);
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
