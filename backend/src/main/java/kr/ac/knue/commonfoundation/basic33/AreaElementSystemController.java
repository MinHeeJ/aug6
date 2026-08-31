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
public class AreaElementSystemController {
    private final AreaElementSystemService service;

    public AreaElementSystemController(AreaElementSystemService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/area-element-systems")
    public ApiResponse<AreaElementSystemSearchResponse> listAreaElementSystems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ruleVersionId,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String elementCode,
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        requireAreaElementSystemAdmin(servletRequest);
        return ApiResponse.ok(service.list(new AreaElementSystemSearchCriteria(page, size, ruleVersionId, areaCode, itemCode, evaluationYear, elementCode, activeYn, keyword)), effectiveRequestId(requestId));
    }

    @PostMapping("/api/admin/area-element-systems/save")
    public ApiResponse<AreaElementSystemRow> saveAreaElementSystem(
            @Valid @RequestBody SaveAreaElementSystemRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireAreaElementSystemAdmin(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireAreaElementSystemAdmin(HttpServletRequest request) {
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
