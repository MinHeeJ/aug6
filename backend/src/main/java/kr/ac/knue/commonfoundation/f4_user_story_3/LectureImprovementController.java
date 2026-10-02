package kr.ac.knue.commonfoundation.f4_user_story_3;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.RequestIdResolver;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Request;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.SaveResponse;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the approved lecture-improvement API while preserving session principal enforcement. */
@RestController
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists the requester's scoped teaching-improvement records. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<SearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireReadUser(request);
        return ApiResponse.ok(service.list(page, pageSize, user), RequestIdResolver.resolve(requestId));
    }

    /** Returns a detail only after the service applies the same data scope as the list. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<Row> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.get(achievementId, requireReadUser(request)),
                RequestIdResolver.resolve(requestId));
    }

    /** Creates an R01-owned teaching-improvement record. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<SaveResponse> create(
            @Valid @RequestBody Request body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String resolvedRequestId = RequestIdResolver.resolve(requestId);
        return ApiResponse.ok(
                service.create(body, requireWriteUser(request), resolvedRequestId),
                resolvedRequestId);
    }

    /** Updates an R01-owned non-finalized teaching-improvement record. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<SaveResponse> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody Request body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String resolvedRequestId = RequestIdResolver.resolve(requestId);
        return ApiResponse.ok(
                service.update(achievementId, body, requireWriteUser(request), resolvedRequestId),
                resolvedRequestId);
    }

    private CurrentUser requireReadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriteUser(HttpServletRequest request) {
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
}
