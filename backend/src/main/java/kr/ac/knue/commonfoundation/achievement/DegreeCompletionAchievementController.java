package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the R01/R02/R04/R09 BASIC-79 degree-completion search and atomic save operations. */
@RestController
public class DegreeCompletionAchievementController {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04", "R09");
    private final DegreeCompletionAchievementService service;

    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service) {
        this.service = service;
    }

    /** Lists only degree-completion rows permitted by the authenticated principal's data scope. */
    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementModels.SearchResponse> listDegreeCompletionAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest servletRequest
    ) {
        CurrentUser user = requireAuthorizedUser(servletRequest);
        validateSearchRequest(page, size);
        return ApiResponse.ok(service.list(
                new DegreeCompletionAchievementModels.SearchCriteria(
                        page,
                        size,
                        managementNo,
                        teacherName,
                        certificationStatus
                ),
                user
        ));
    }

    /** Saves the header and recipient sub-table in one service transaction. */
    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionAchievementModels.Row> saveDegreeCompletionAchievement(
            @RequestBody DegreeCompletionSaveRequest request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser user = requireAuthorizedUser(servletRequest);
        validateSaveRequest(request);
        return ApiResponse.ok(service.save(request, user));
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
            throw new BusinessValidationException("석·박사 배출 검색조건이 올바르지 않습니다.", fields);
        }
    }

    private void validateSaveRequest(DegreeCompletionSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.getManagementItemCode() == null
                || request.getManagementItemCode().isBlank()) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        }
        if (request == null || request.getStudents() == null || request.getStudents().isEmpty()) {
            fields.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        } else {
            for (DegreeCompletionSaveRequest.StudentRequest student : request.getStudents()) {
                if (student == null || student.getDegreeType() == null || student.getDegreeType().isBlank()) {
                    fields.add(new ValidationError("degreeType", "학위구분을 입력하세요."));
                }
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("석·박사 배출 저장 요청이 올바르지 않습니다.", fields);
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
