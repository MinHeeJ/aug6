package kr.ac.knue.commonfoundation.basic60;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    @PostMapping("/api/admin/participation-allocation-rate-settings/save")
    public ApiResponse<OperationalSettingRow> saveParticipationAllocationRateSetting(
            @Valid @RequestBody SaveParticipationAllocationRateSettingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSettingsAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.saveParticipationSetting(request, user.userId(), effectiveRequestId), effectiveRequestId);
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

    @PostMapping("/api/admin/management-item-evaluation-score-settings/save")
    public ApiResponse<OperationalSettingRow> saveManagementItemEvaluationScoreSetting(
            @Valid @RequestBody SaveManagementItemEvaluationScoreSettingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSettingsAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.saveScoreSetting(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    @GetMapping("/api/faculty/course-area-group-grades")
    public ApiResponse<CourseAreaGroupGradeSearchResponse> listCourseAreaGroupGrades(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String completionType, @RequestParam(required = false) String semester,
            @RequestParam(required = false) String courseArea, @RequestParam(required = false) Long facultyUserId,
            @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireGradeReader(servletRequest);
        validatePageSize(pageSize);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.listCourseAreaGroupGrades(new CourseAreaGroupGradeSearchCriteria(page, pageSize, completionType, semester, courseArea, facultyUserId, keyword), user, effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireSettingsAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R09")) return currentUser;
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private CurrentUser requireGradeReader(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().stream().anyMatch(role -> java.util.Set.of("R01", "R04", "R08", "R09").contains(role))) return currentUser;
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", java.util.List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
