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
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Request;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SaveResult;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchCriteria;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entrypoint for the approved BASIC-83 lecture-improvement operations.
 * It establishes route-level roles before dispatching scoped reads or guarded writes.
 */
@RestController
public class LectureImprovementAchievementController {
    private final LectureImprovementAchievementService service;

    public LectureImprovementAchievementController(LectureImprovementAchievementService service) {
        this.service = service;
    }

    /** Lists caller-visible lecture-improvement records with approved pagination sizes. */
    @GetMapping("/api/business/lecture-improvements")
    public ApiResponse<SearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePage(page);
        validatePageSize(pageSize);
        CurrentUser user = requireReadRole(request);
        return ApiResponse.ok(
                service.list(new SearchCriteria(page, pageSize), user),
                effectiveRequestId(requestId));
    }

    /** Gets one caller-visible lecture-improvement record. */
    @GetMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<Row> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, requireReadRole(request)), effectiveRequestId(requestId));
    }

    /** Creates a lecture-improvement record through the shared guarded transaction. */
    @PostMapping("/api/business/lecture-improvements")
    public ApiResponse<SaveResult> create(
            @Valid @RequestBody Request body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireWriteRole(request);
        validateSemester(body);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, user, traceId), traceId);
    }

    /** Updates a lecture-improvement record through the shared guarded transaction. */
    @PutMapping("/api/business/lecture-improvements/{achievementId}")
    public ApiResponse<SaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody Request body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireWriteRole(request);
        validateSemester(body);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, user, traceId), traceId);
    }

    private CurrentUser requireReadRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
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

    private void validateSemester(Request body) {
        if (body != null && body.semester() != null && body.semester() != 1 && body.semester() != 2) {
            throw new BusinessValidationException(
                    "강의개선 실적 저장 요청이 올바르지 않습니다.",
                    List.of(new ValidationError("semester", "학기는 1 또는 2여야 합니다.")));
        }
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new BusinessValidationException(
                    "목록 페이지가 올바르지 않습니다.",
                    List.of(new ValidationError("page", "페이지는 0 이상이어야 합니다.")));
        }
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
