package kr.ac.knue.commonfoundation.basic83;

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

/** Exposes the approved BASIC-83 teaching-improvement HTTP contract. */
@RestController
public class LectureImprovementAchievementController {
    private final LectureImprovementAchievementService service;

    public LectureImprovementAchievementController(LectureImprovementAchievementService service) {
        this.service = service;
    }

    /** Lists caller-scoped rows with one of the contractually allowed page sizes. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        String traceId = traceId(requestId);
        return ApiResponse.ok(
                service.list(new LectureImprovementSearchCriteria(page, pageSize), readUser(request)),
                traceId
        );
    }

    /** Returns one detail row only if the requester has an authorized data scope. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementAchievementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, readUser(request)), traceId(requestId));
    }

    /** Creates an R01-owned DRAFT teaching-improvement achievement. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSaveResult> create(
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, writeUser(request), traceId), traceId);
    }

    /** Updates an R01-owned achievement through its guarded transactional service. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, writeUser(request), traceId), traceId);
    }

    private CurrentUser readUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || user.roles().stream()
                .noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser writeUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser current)) {
            throw new UnauthenticatedException();
        }
        return current;
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요."))
            );
        }
    }

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
