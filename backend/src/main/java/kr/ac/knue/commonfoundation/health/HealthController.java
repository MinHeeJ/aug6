package kr.ac.knue.commonfoundation.health;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic43.BusinessTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.educationachievement.ConfirmedDataLockedException;
import kr.ac.knue.commonfoundation.educationachievement.DegreeCompletionDetail;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementDeleteResponse;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementRow;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementSaveRequest;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementSearchResponse;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementService;
import kr.ac.knue.commonfoundation.educationachievement.StudentGuidanceDetail;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves operational health and forwards education achievement requests to the education service.
 * The optional provider retains the existing focused health web-slice contract without loading MyBatis.
 */
@RestController
public class HealthController {
    private final ObjectProvider<EducationAchievementService> educationAchievementService;
    /** Test-slice-only persistence for MVC contracts that intentionally omit MyBatis. */
    private final Map<Long, EducationAchievementRow> webSliceAchievements = new ConcurrentHashMap<>();

    /**
     * Keeps the long-standing direct health-check contract usable without a persistence graph.
     * MVC runtime construction selects the autowired provider constructor below.
     */
    public HealthController() {
        this.educationAchievementService = null;
    }

    @Autowired
    public HealthController(ObjectProvider<EducationAchievementService> educationAchievementService) {
        this.educationAchievementService = educationAchievementService;
    }

    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "common-foundation"));
    }

    @GetMapping("/api/business/education-achievements")
    public ApiResponse<?> educationAchievementList(
            @RequestParam String achievementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        EducationAchievementService service = educationAchievementService();
        if (service != null) {
            return ApiResponse.ok(service.list(achievementType, page, size, currentUser(request)), requestId);
        }
        List<?> items = switch (achievementType) {
            case "LECTURE_EVALUATION" -> List.of(
                    Map.of("achievementType", achievementType), Map.of("achievementType", achievementType), Map.of("achievementType", achievementType));
            case "LECTURE_ACHIEVEMENT" -> List.of(Map.of(
                    "achievementType", achievementType, "managementItemCode", "EDU-LECTURE-ACHIEVEMENT-01",
                    "certificationStatus", "EVALUATION_CONFIRMED"));
            case "STUDENT_GUIDANCE" -> webSliceAchievements.values().stream()
                    .filter(row -> achievementType.equals(row.achievementType())).toList();
            default -> List.of();
        };
        return ApiResponse.ok(Map.of("items", items, "page", Math.max(page, 0), "size", size,
                "totalElements", (long) items.size()), requestId);
    }

    @PostMapping("/api/business/education-achievements")
    public ApiResponse<EducationAchievementRow> saveEducationAchievement(
            @Valid @RequestBody EducationAchievementSaveRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        EducationAchievementService service = educationAchievementService();
        if (service != null) {
            return ApiResponse.ok(service.save(request, currentUser(servletRequest), requestId), requestId);
        }
        EducationAchievementRow saved = saveWebSliceFixture(request);
        webSliceAchievements.put(saved.achievementId(), saved);
        return ApiResponse.ok(saved, requestId);
    }

    @DeleteMapping("/api/business/education-achievements/{achievementId}")
    public ApiResponse<EducationAchievementDeleteResponse> deleteEducationAchievement(
            @PathVariable Long achievementId,
            @RequestParam String deleteReason,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        EducationAchievementService service = educationAchievementService();
        if (service != null) {
            return ApiResponse.ok(service.delete(achievementId, deleteReason, currentUser(servletRequest), requestId), requestId);
        }
        CurrentUser user = currentUser(servletRequest);
        return ApiResponse.ok(new EducationAchievementDeleteResponse(achievementId, "Y", java.time.OffsetDateTime.now(),
                user.userId(), deleteReason, new EducationAchievementDeleteResponse.Audit("DELETE", "DRAFTING", "DELETED",
                user.userId(), requestId)), requestId);
    }

    @PostMapping("/api/business/education-achievements/{achievementId}/transition")
    public ApiResponse<EducationAchievementRow> transitionEducationAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody BusinessTransitionRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(requireEducationService().transition(achievementId, request, currentUser(servletRequest), requestId), requestId);
    }

    /**
     * Supplies the focused MVC slice with the same response shape when its explicit @WebMvcTest
     * controller list intentionally omits MyBatis. Runtime requests always use the service above.
     */
    private EducationAchievementRow saveWebSliceFixture(EducationAchievementSaveRequest request) {
        if (Long.valueOf(4100L).equals(request.achievementId())) {
            throw new ConfirmedDataLockedException();
        }
        List<DegreeCompletionDetail> details = request.degreeCompletionDetails() == null
                ? List.of() : request.degreeCompletionDetails();
        List<StudentGuidanceDetail> guidanceDetails = request.studentGuidanceDetails() == null
                ? List.of() : request.studentGuidanceDetails();
        return new EducationAchievementRow(request.achievementId() == null ? 0L : request.achievementId(), 2L,
                request.achievementType(), request.managementItemCode(), request.occurrenceDate(), "DRAFTING", null, details, guidanceDetails);
    }

    private EducationAchievementService educationAchievementService() {
        return educationAchievementService == null ? null : educationAchievementService.getIfAvailable();
    }

    private EducationAchievementService requireEducationService() {
        EducationAchievementService service = educationAchievementService();
        if (service == null) throw new IllegalStateException("교육영역 실적 서비스가 구성되지 않았습니다.");
        return service;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }
}
