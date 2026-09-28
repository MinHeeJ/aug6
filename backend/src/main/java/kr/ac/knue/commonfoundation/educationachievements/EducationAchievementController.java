package kr.ac.knue.commonfoundation.educationachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** HTTP boundary for BASIC-72 education achievements, preserving the established session principal model. */
@RestController
public class EducationAchievementController {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04", "R09");
    private static final Set<String> WRITE_ROLES = Set.of("R01", "R09");
    private final EducationAchievementService service;
    public EducationAchievementController(EducationAchievementService service) { this.service = service; }

    @GetMapping("/api/business/lecture-evaluations")
    public ApiResponse<EducationAchievementPage> listLectureEvaluations(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) { return ApiResponse.ok(service.listLectureEvaluations(page, size, requireRole(request, READ_ROLES))); }
    @PostMapping("/api/business/lecture-evaluations")
    public ApiResponse<EducationAchievement> saveLectureEvaluation(@Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.saveLectureEvaluation(body, requireRole(request, WRITE_ROLES))); }
    @PatchMapping("/api/business/lecture-evaluations/{achievementId}")
    public ApiResponse<EducationAchievement> updateLectureEvaluation(@PathVariable Long achievementId, @Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.updateAchievement(achievementId, body, requireRole(request, WRITE_ROLES))); }

    @GetMapping("/api/business/lecture-achievements")
    public ApiResponse<EducationAchievementPage> listLectureAchievements(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) { return ApiResponse.ok(service.listLectureAchievements(page, size, requireRole(request, READ_ROLES))); }
    @PostMapping("/api/business/lecture-achievements")
    public ApiResponse<EducationAchievement> saveLectureAchievement(@Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.saveLectureAchievement(body, requireRole(request, WRITE_ROLES))); }
    @PatchMapping("/api/business/lecture-achievements/{achievementId}")
    public ApiResponse<EducationAchievement> updateLectureAchievement(@PathVariable Long achievementId, @Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.updateAchievement(achievementId, body, requireRole(request, WRITE_ROLES))); }

    @GetMapping("/api/business/student-guidance-achievements")
    public ApiResponse<EducationAchievementPage> listStudentGuidanceAchievements(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) { return ApiResponse.ok(service.listStudentGuidanceAchievements(page, size, requireRole(request, READ_ROLES))); }
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<EducationAchievement> saveStudentGuidanceAchievement(@Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.saveStudentGuidanceAchievement(body, requireRole(request, WRITE_ROLES))); }

    @PostMapping("/api/business/student-guidance-achievements/excel/uploads")
    public ApiResponse<StudentGuidanceUploadResult> uploadStudentGuidanceExcel(@RequestParam("file") MultipartFile file, HttpServletRequest request) { return ApiResponse.ok(service.validateStudentGuidanceUpload(file, requireRole(request, Set.of("R07", "R09")))); }

    @GetMapping("/api/business/graduate-achievements")
    public ApiResponse<EducationAchievementPage> listGraduateAchievements(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) { return ApiResponse.ok(service.listGraduateAchievements(page, size, requireRole(request, READ_ROLES))); }
    @PostMapping("/api/business/graduate-achievements")
    public ApiResponse<EducationAchievement> saveGraduateAchievement(@Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.saveGraduateAchievement(body, requireRole(request, WRITE_ROLES))); }
    @PatchMapping("/api/business/graduate-achievements/{achievementId}")
    public ApiResponse<EducationAchievement> updateGraduateAchievement(@PathVariable Long achievementId, @Valid @RequestBody SaveLectureEvaluationRequest body, HttpServletRequest request) { return ApiResponse.ok(service.updateAchievement(achievementId, body, requireRole(request, WRITE_ROLES))); }
    @GetMapping("/api/business/graduate-achievements/{achievementId}")
    public ApiResponse<EducationAchievement> getGraduateAchievement(@PathVariable Long achievementId, HttpServletRequest request) { return ApiResponse.ok(service.findAchievement(achievementId, requireRole(request, READ_ROLES))); }

    private CurrentUser requireRole(HttpServletRequest request, Set<String> roles) {
        Object principal = request.getAttribute("currentUser");
        if (!(principal instanceof CurrentUser user)) throw new UnauthenticatedException();
        if (user.roles().stream().noneMatch(roles::contains)) throw new ForbiddenException();
        return user;
    }
}
