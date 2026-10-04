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
 * Exposes the approved lecture-improvement API with separate reader and writer
 * role boundaries before the service applies record-level data scope and locks.
 */
@RestController
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Lists R01/R02/R04-visible lecture improvements using the approved pagination options. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireReader(request);
        return ApiResponse.ok(
                service.list(new LectureImprovementSearchCriteria(page, pageSize), user),
                effectiveRequestId(requestId));
    }

    /** Retrieves one R01/R02/R04-visible lecture improvement through service data scoping. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireReader(request);
        return ApiResponse.ok(service.get(achievementId, user), effectiveRequestId(requestId));
    }

    /** Creates a draft lecture improvement; only R01 may initiate a mutation. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<LectureImprovementRow> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireWriter(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, user, traceId), traceId);
    }

    /** Updates a mutable lecture improvement; finalized records remain locked in the service transaction. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<LectureImprovementRow> updateLectureImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireWriter(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user, traceId), traceId);
    }

    private CurrentUser requireReader(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriter(HttpServletRequest request) {
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
        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId.trim();
    }
}
