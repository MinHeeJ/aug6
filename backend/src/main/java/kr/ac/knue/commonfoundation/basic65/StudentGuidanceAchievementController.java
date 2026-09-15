package kr.ac.knue.commonfoundation.basic65;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class StudentGuidanceAchievementController {
    private final StudentGuidanceAchievementService service;
    public StudentGuidanceAchievementController(StudentGuidanceAchievementService service) { this.service = service; }
    @GetMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementSearchResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String academicYear, @RequestParam(required = false) String semester,
            @RequestParam(required = false) String studentKeyword, @RequestParam(required = false) String guidanceType, @RequestParam(required = false) String achievementStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireReader(request);
        return ApiResponse.ok(service.list(new StudentGuidanceAchievementSearchCriteria(page, size, evaluationYear, academicYear, semester, studentKeyword, guidanceType, achievementStatus), user.userId()), requestId(requestId));
    }
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementRow> save(@Valid @RequestBody SaveStudentGuidanceAchievementRequest body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = currentUser(request); if (!user.roles().contains("R01")) throw new ForbiddenException(); String id = requestId(requestId); return ApiResponse.ok(service.save(body, user.userId(), id), id);
    }
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentGuidanceExcelUploadResult> upload(@RequestParam(required = false) String templateId, @RequestPart("file") MultipartFile file, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = currentUser(request); if (!user.roles().contains("R07")) throw new ForbiddenException(); String id = requestId(requestId); return ApiResponse.ok(service.upload(templateId, file, user.userId(), id), id);
    }
    private CurrentUser requireReader(HttpServletRequest request) { CurrentUser user = currentUser(request); if (user.roles().stream().anyMatch(r -> r.equals("R01") || r.equals("R02") || r.equals("R04") || r.equals("R07"))) return user; throw new ForbiddenException(); }
    private CurrentUser currentUser(HttpServletRequest request) { Object user = request.getAttribute("currentUser"); if (user instanceof CurrentUser currentUser) return currentUser; throw new UnauthenticatedException(); }
    private String requestId(String requestId) { return requestId != null && !requestId.isBlank() ? requestId.trim() : UUID.randomUUID().toString(); }
}
