package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the approved lecture-improvement list, detail, create, and update
 * HTTP operations while keeping authentication at the transport boundary.
 */
@RestController
public class LectureImprovementController {
    private static final List<String> READ_ROLES = List.of("R01", "R02", "R04");
    private static final List<String> WRITE_ROLES = List.of("R01");
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists caller-scoped lecture improvements using the approved page-size values. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePagination(page, pageSize);
        return ApiResponse.ok(service.list(page, pageSize, requireUser(request, READ_ROLES)), requestId(requestId));
    }

    /** Returns one lecture-improvement record after service-level data-scope verification. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireUser(request, READ_ROLES)), requestId(requestId));
    }

    /** Creates a lecture-improvement record in the guarded header/detail transaction. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementRow> create(
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireUser(request, WRITE_ROLES);
        String effectiveRequestId = requestId(requestId);
        return ApiResponse.ok(service.create(body, user, effectiveRequestId), effectiveRequestId);
    }

    /** Updates a lecture-improvement record only after ownership and lifecycle guards pass. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementRow> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireUser(request, WRITE_ROLES);
        String effectiveRequestId = requestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user, effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireUser(HttpServletRequest request, List<String> allowedRoles) {
        Object currentUser = request.getAttribute("currentUser");
        if (!(currentUser instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(allowedRoles::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 0) {
            throw new BusinessValidationException(
                    "페이지 번호가 올바르지 않습니다.",
                    List.of(new ValidationError("page", "0 이상을 입력하세요.")));
        }
        if (!List.of(20, 50, 100).contains(pageSize)) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String requestId(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }
}
