package kr.ac.knue.commonfoundation.educationachievement;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shared HTTP boundary for the education-achievement module.
 *
 * <p>Story slices add their OpenAPI operations to this controller while all responses use the
 * existing API envelope and a request identifier supplied by the established SessionCookie flow.</p>
 */
@RestController
public class EducationAchievementController {
    private final EducationAchievementService service;

    public EducationAchievementController(EducationAchievementService service) {
        this.service = service;
    }

    /**
     * Returns the first-stage lecture-evaluation collection using the shared response envelope.
     *
     * <p>The authenticated SessionCookie and menu permission are enforced by the existing filter;
     * this endpoint supplies the page metadata required for the list-to-detail workflow while the
     * story's row query is added in a subsequent contract test.</p>
     *
     * @param page zero-based requested page
     * @param pageSize one of the supported list page sizes
     * @param request HTTP request used to propagate the request identifier
     * @return page metadata and an empty row collection for the initial list contract
     */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<Map<String, Object>> listLectureEvaluationAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new IllegalArgumentException("목록 건수는 20, 50, 100 중 하나여야 합니다.");
        }
        int totalElements = service.countActiveByAchievementType("LECTURE_EVALUATION");
        return success(Map.of(
                "page", page,
                "pageSize", pageSize,
                "totalElements", totalElements,
                "items", Collections.emptyList()), request);
    }

    /**
     * Returns the first page of lecture-performance achievements for the FR-026 entry flow.
     *
     * <p>The collection shares the established pagination envelope with lecture evaluations so
     * the frontend can render its search, list, and later detail flow without bypassing the
     * education-achievement authorization boundary.</p>
     *
     * @param page zero-based requested page
     * @param pageSize one of the supported list page sizes
     * @param request HTTP request used to propagate the request identifier
     * @return page metadata and the initial collection payload
     */
    @GetMapping("/api/business/lecture-performance-achievements")
    public ApiResponse<Map<String, Object>> listLecturePerformanceAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new IllegalArgumentException("목록 건수는 20, 50, 100 중 하나여야 합니다.");
        }
        int totalElements = service.countActiveByAchievementType("LECTURE_PERFORMANCE");
        return success(Map.of(
                "page", page,
                "pageSize", pageSize,
                "totalElements", totalElements,
                "items", Collections.emptyList()), request);
    }

    /**
     * Returns the first page of student-guidance achievements for the FR-027 entry flow.
     *
     * <p>The collection uses the existing education-achievement page envelope so an authorized
     * faculty member can begin the list-to-detail journey without bypassing the shared request-id
     * and authorization boundary. Student-guidance save and Excel operations remain separate
     * contract slices.</p>
     *
     * @param page zero-based requested page
     * @param pageSize one of the supported list page sizes
     * @param request HTTP request used to propagate the request identifier
     * @return page metadata and the initial collection payload
     */
    @GetMapping("/api/business/student-guidance-achievements")
    public ApiResponse<Map<String, Object>> listStudentGuidanceAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new IllegalArgumentException("목록 건수는 20, 50, 100 중 하나여야 합니다.");
        }
        int totalElements = service.countActiveByAchievementType("STUDENT_GUIDANCE");
        return success(Map.of(
                "page", page,
                "pageSize", pageSize,
                "totalElements", totalElements,
                "items", Collections.emptyList()), request);
    }

    /**
     * Returns the first page of graduate-degree achievements for the FR-028 list-to-detail flow.
     *
     * <p>The shared page envelope lets the authorized screen show its real result count before a
     * selected achievement reveals the associated graduate student details.</p>
     *
     * @param page zero-based requested page
     * @param pageSize one of the supported list page sizes
     * @param request HTTP request used to propagate the request identifier
     * @return page metadata and the initial collection payload
     */
    @GetMapping("/api/business/graduate-degree-achievements")
    public ApiResponse<Map<String, Object>> listGraduateDegreeAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new IllegalArgumentException("목록 건수는 20, 50, 100 중 하나여야 합니다.");
        }
        int totalElements = service.countActiveByAchievementType("GRADUATE_DEGREE");
        return success(Map.of(
                "page", page,
                "pageSize", pageSize,
                "totalElements", totalElements,
                "items", Collections.emptyList()), request);
    }

    /**
     * Builds a standard API envelope with the request identifier that later operations persist in
     * status and change histories.
     *
     * @param data response payload
     * @param request HTTP request carrying an optional {@code X-Request-Id}
     * @param <T> response payload type
     * @return standard success envelope with request metadata
     */
    protected <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, service.requestId(request.getHeader("X-Request-Id")));
    }
}
