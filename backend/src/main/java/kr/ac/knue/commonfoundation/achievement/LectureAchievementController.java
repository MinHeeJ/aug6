package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

/** Exposes the R01/R02/R04/R09 lecture-achievement list and save operations defined by BASIC-79. */
@RestController
public class LectureAchievementController {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04", "R09");
    private final LectureAchievementService service;

    public LectureAchievementController(LectureAchievementService service) {
        this.service = service;
    }

    /** Lists the current user's permitted lecture-achievement achievement rows. */
    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementModels.SearchResponse> listLectureAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) LocalDate occurredDateFrom,
            @RequestParam(required = false) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest servletRequest
    ) {
        CurrentUser user = requireAuthorizedUser(servletRequest);
        validateSearchRequest(page, size);
        return ApiResponse.ok(service.list(new LectureAchievementModels.SearchCriteria(
                page,
                size,
                managementNo,
                teacherName,
                managementItemCode,
                occurredDateFrom,
                occurredDateTo,
                certificationStatus
        ), user));
    }

    /** Saves an authenticated user's lecture-achievement source row and its mandatory audit history. */
    @PostMapping("/api/business/lecture-achievements")
    public ApiResponse<LectureAchievementModels.Row> saveLectureAchievement(
            @RequestBody LectureSaveRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String ignoredRequestId,
            HttpServletRequest servletRequest
    ) {
        CurrentUser user = requireAuthorizedUser(servletRequest);
        validateSaveRequest(request);
        return ApiResponse.ok(service.save(request, user));
    }

    private void validateSaveRequest(LectureSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.getManagementItemCode() == null
                || request.getManagementItemCode().isBlank()) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        }
        if (request == null || request.getOccurredDate() == null) {
            fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의실적 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private void validateSearchRequest(int page, int size) {
        List<ValidationError> fields = new ArrayList<>();
        if (page < 0) {
            fields.add(new ValidationError("page", "페이지 번호는 0 이상이어야 합니다."));
        }
        if (size != 20 && size != 50 && size != 100) {
            fields.add(new ValidationError("size", "목록 건수는 20, 50 또는 100이어야 합니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("강의실적 검색조건이 올바르지 않습니다.", fields);
        }
    }

    private CurrentUser requireAuthorizedUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) {
            throw new UnauthenticatedException();
        }
        if (currentUser.roles().stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new ForbiddenException();
        }
        return currentUser;
    }
}
