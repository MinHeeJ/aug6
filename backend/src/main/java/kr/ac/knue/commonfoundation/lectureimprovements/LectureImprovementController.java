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

/** Exposes the OpenAPI-approved lecture-improvement list, detail, create, and update operations. */
@RestController
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists lecture improvements for an R01, R02, or R04 session and its permitted data scope. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser currentUser = requireRole(request, List.of("R01", "R02", "R04"));
        return ApiResponse.ok(service.list(page, pageSize, currentUser), effectiveRequestId(requestId));
    }

    /** Reads one lecture-improvement source row after the same server-side data-scope checks. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementAchievement> getLectureImprovement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireRole(request, List.of("R01", "R02", "R04"));
        return ApiResponse.ok(service.get(achievementId, currentUser), effectiveRequestId(requestId));
    }

    /** Creates an R01-owned lecture-improvement achievement through the transaction-owning service. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementAchievement> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireRole(request, List.of("R01"));
        return ApiResponse.ok(service.create(body, currentUser), effectiveRequestId(requestId));
    }

    /** Updates an R01-owned, non-finalized lecture-improvement achievement. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementAchievement> updateLectureImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser currentUser = requireRole(request, List.of("R01"));
        return ApiResponse.ok(service.update(achievementId, body, currentUser), effectiveRequestId(requestId));
    }

    private CurrentUser requireRole(HttpServletRequest request, List<String> allowedRoles) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || user.roles().stream().noneMatch(allowedRoles::contains)) {
            throw new ForbiddenException();
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
