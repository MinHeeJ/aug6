package kr.ac.knue.commonfoundation.basic34;

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
public class EvaluationRuleSetController {
    private final EvaluationRuleSetService service;

    public EvaluationRuleSetController(EvaluationRuleSetService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/evaluation-rule-sets")
    public ApiResponse<EvaluationRuleSetSearchResponse> listEvaluationRuleSets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId,
            @RequestParam(required = false) String targetScope,
            @RequestParam(required = false) String ruleSetName,
            @RequestParam(required = false) String ruleSetStatus,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireEvaluationRuleSetAdmin(servletRequest);
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException("업적평가 기준·점수규칙 목록 표시 건수가 올바르지 않습니다.",
                    java.util.List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
        return ApiResponse.ok(service.list(new EvaluationRuleSetSearchCriteria(page, pageSize, ruleVersionId,
                targetScope, ruleSetName, ruleSetStatus, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/evaluation-rule-sets")
    public ApiResponse<EvaluationRuleSetRow> saveEvaluationRuleSetContractOperation(
            @Valid @RequestBody SaveEvaluationRuleSetRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        return saveEvaluationRuleSet(request, requestId, servletRequest);
    }

    @PostMapping("/api/admin/evaluation-rule-sets/save")
    public ApiResponse<EvaluationRuleSetRow> saveEvaluationRuleSet(
            @Valid @RequestBody SaveEvaluationRuleSetRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireEvaluationRuleSetAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireEvaluationRuleSetAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R08") || currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
