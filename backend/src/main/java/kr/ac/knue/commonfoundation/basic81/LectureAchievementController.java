package kr.ac.knue.commonfoundation.basic81;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the approved lecture-achievement list and create/update HTTP operations. */
@RestController
public class LectureAchievementController {
    private final LectureAchievementService service;

    public LectureAchievementController(LectureAchievementService service) {
        this.service = service;
    }

    /** Lists lecture achievements after session role verification and service-level data scoping. */
    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementSearchResponse> listLectureAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireAchievementRole(request);
        return ApiResponse.ok(
                service.list(new LectureAchievementSearchCriteria(
                        page,
                        pageSize,
                        managementNo,
                        teacherName,
                        managementItemCode,
                        certificationStatus), user),
                effectiveRequestId(requestId));
    }

    /** Saves a lecture achievement through the guarded atomic service transaction. */
    @PostMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementSaveResult> saveLectureAchievement(
            @Valid @RequestBody SaveLectureAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireAchievementRole(request);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(body, user, traceId), traceId);
    }

    private CurrentUser requireAchievementRole(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
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
