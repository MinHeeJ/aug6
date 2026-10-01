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

/** Exposes the approved degree-completion list and atomic header/detail save operations. */
@RestController
public class DegreeCompletionAchievementController {
    private final DegreeCompletionAchievementService service;

    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service) {
        this.service = service;
    }

    /** Lists degree-completion records after route-level role verification and service data scoping. */
    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementSearchResponse> listDegreeCompletionAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireAchievementRole(request);
        return ApiResponse.ok(
                service.list(
                        new DegreeCompletionAchievementSearchCriteria(
                                page,
                                pageSize,
                                managementNo,
                                teacherName,
                                certificationStatus),
                        user),
                effectiveRequestId(requestId));
    }

    /** Saves a degree-completion header and all supplied students in one transaction. */
    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementRow> saveDegreeCompletionAchievement(
            @Valid @RequestBody SaveDegreeCompletionAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        CurrentUser user = requireAchievementRole(request);
        validateStudentDetails(body);
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.save(body, user, traceId), traceId);
    }

    /** Validates nested student fields before dispatching a command to the transactional service. */
    private void validateStudentDetails(SaveDegreeCompletionAchievementRequest request) {
        if (request == null || request.students() == null) {
            return;
        }
        List<ValidationError> errors = new java.util.ArrayList<>();
        for (DegreeCompletionStudentRequest student : request.students()) {
            if (student == null || student.degreeType() == null || student.degreeType().isBlank()) {
                errors.add(new ValidationError("degreeType", "학위구분을 입력하세요."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("석·박사 배출 저장 요청이 올바르지 않습니다.", errors);
        }
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
