package kr.ac.knue.commonfoundation.basic33;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationAreaController {
    private final EvaluationAreaService service;

    public EvaluationAreaController(EvaluationAreaService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/evaluation-areas")
    public ApiResponse<EvaluationAreaSearchResponse> listEvaluationAreas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ruleVersionId,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireEvaluationAreaAdmin(servletRequest);
        return ApiResponse.ok(service.list(new EvaluationAreaSearchCriteria(page, size, ruleVersionId, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/evaluation-areas/save")
    public ApiResponse<EvaluationAreaRow> saveEvaluationArea(
            @Valid @RequestBody SaveEvaluationAreaRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireEvaluationAreaAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireEvaluationAreaAdmin(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString();
    }
}
