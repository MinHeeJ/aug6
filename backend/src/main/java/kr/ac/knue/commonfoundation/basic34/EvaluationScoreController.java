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
public class EvaluationScoreController {
    private final EvaluationScoreService service;

    public EvaluationScoreController(EvaluationScoreService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/evaluation-scores")
    public ApiResponse<EvaluationScoreSearchResponse> listEvaluationScores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleVersionId,
            @RequestParam(required = false) Long managementItemId,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireEvaluationScoreAdmin(servletRequest);
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException("평가점수 목록 표시 건수가 올바르지 않습니다.",
                    java.util.List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
        return ApiResponse.ok(service.list(new EvaluationScoreSearchCriteria(page, pageSize, ruleVersionId,
                managementItemId, areaCode, itemCode, evaluationYear, elementCode, managementItemCode,
                organizationCode, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/evaluation-scores")
    public ApiResponse<EvaluationScoreRow> saveEvaluationScoreContractOperation(
            @Valid @RequestBody SaveEvaluationScoreRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        return saveEvaluationScore(request, requestId, servletRequest);
    }

    @PostMapping("/api/admin/evaluation-scores/save")
    public ApiResponse<EvaluationScoreRow> saveEvaluationScore(
            @Valid @RequestBody SaveEvaluationScoreRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireEvaluationScoreAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireEvaluationScoreAdmin(HttpServletRequest request) {
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
