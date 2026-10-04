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

/** Exposes the approved lecture-improvement create, list, detail, and update HTTP contract. */
@RestController
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists only the lecture-improvement rows available in the caller's data scope. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireReadRole(request);
        return ApiResponse.ok(
                service.list(new LectureImprovementSearchCriteria(page, pageSize), user),
                effectiveRequestId(requestId));
    }

    /** Returns one scoped lecture-improvement achievement for the selected row. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireReadRole(request)), effectiveRequestId(requestId));
    }

    /** Creates a DRAFT lecture-improvement achievement for the authenticated R01 faculty member. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSaveResponse> create(
            @Valid @RequestBody LectureImprovementSaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, requireWriteRole(request), traceId), traceId);
    }

    /** Updates the path-selected achievement only after R01 ownership and lifecycle validation. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementSaveResponse> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementSaveRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, requireWriteRole(request), traceId), traceId);
    }

    private CurrentUser requireReadRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || user.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriteRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
