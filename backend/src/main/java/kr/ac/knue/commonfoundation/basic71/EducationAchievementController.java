package kr.ac.knue.commonfoundation.basic71;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Exposes the faculty education-achievement APIs defined by the approved contract. */
@RestController
public class EducationAchievementController {
    private final EducationAchievementService service;

    public EducationAchievementController(EducationAchievementService service) { this.service = service; }

    @GetMapping("/api/faculty/education-achievements")
    public ApiResponse<EducationAchievementSearchResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String achievementType, HttpServletRequest request) {
        CurrentUser user = requireAny(request, "R01", "R02", "R04");
        return ApiResponse.ok(service.list(new EducationAchievementSearchCriteria(page, size, achievementType), user));
    }

    @PostMapping("/api/faculty/education-achievements")
    public ApiResponse<EducationAchievementRow> create(@RequestBody SaveEducationAchievementRequest body, HttpServletRequest request) {
        CurrentUser user = requireAny(request, "R01");
        validateCreate(body);
        return ApiResponse.ok(service.create(body, user.userId(), requestId(request)), requestId(request));
    }

    @GetMapping("/api/faculty/education-achievements/{achievementId}")
    public ApiResponse<EducationAchievementRow> get(@PathVariable Long achievementId, HttpServletRequest request) {
        CurrentUser user = requireAny(request, "R01", "R02", "R04");
        return ApiResponse.ok(service.get(achievementId, user));
    }

    @PostMapping("/api/faculty/education-achievements/{achievementId}/transition")
    public ApiResponse<EducationAchievementRow> transition(@PathVariable Long achievementId, @RequestBody EducationAchievementTransitionRequest body, HttpServletRequest request) {
        CurrentUser user = requireAny(request, "R01", "R02", "R04");
        validateTransition(body);
        return ApiResponse.ok(service.transition(achievementId, body, user, requestId(request)), requestId(request));
    }

    @PostMapping(value = "/api/faculty/student-guidance-achievements/excel-uploads", consumes = "multipart/form-data")
    public ApiResponse<StudentGuidanceUploadResult> uploadStudentGuidance(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        CurrentUser user = requireAny(request, "R07");
        return ApiResponse.ok(service.validateStudentGuidanceUpload(file, user.userId(), requestId(request)), requestId(request));
    }

    private void validateCreate(SaveEducationAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || blank(request.achievementType())) fields.add(new ValidationError("achievementType", "실적유형을 선택하세요."));
        if (request == null || blank(request.managementItemCode())) fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        if (request == null || request.achievementOccurredOn() == null) fields.add(new ValidationError("achievementOccurredOn", "업적발생일을 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("교육영역 실적 저장 요청이 올바르지 않습니다.", fields);
    }

    private void validateTransition(EducationAchievementTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String action = request == null || request.actionType() == null ? "" : request.actionType().trim().toUpperCase();
        if (action.isBlank()) fields.add(new ValidationError("actionType", "처리구분을 선택하세요."));
        if ("DEPARTMENT_REJECT".equals(action) || "CERTIFICATION_REJECT".equals(action)) {
            if (request.opinion() == null || request.opinion().isBlank()) fields.add(new ValidationError("opinion", "미승인 또는 반려 의견을 입력하세요."));
            if (request.reasonCode() == null || request.reasonCode().isBlank()) fields.add(new ValidationError("reasonCode", "미승인 또는 반려 사유를 선택하세요."));
        }
        if (!fields.isEmpty()) throw new BusinessValidationException("교육영역 실적 상태전이 요청이 올바르지 않습니다.", fields);
    }

    private CurrentUser requireAny(HttpServletRequest request, String... roles) {
        Object value = request.getAttribute("currentUser");
        if (!(value instanceof CurrentUser user)) throw new UnauthenticatedException();
        for (String role : roles) if (user.roles().contains(role)) return user;
        throw new ForbiddenException();
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String requestId(HttpServletRequest request) { String value = request.getHeader("X-Request-Id"); return value == null || value.isBlank() ? "AUTO" : value.trim(); }
}
