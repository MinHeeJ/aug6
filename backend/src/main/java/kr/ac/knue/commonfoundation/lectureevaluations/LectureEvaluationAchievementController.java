package kr.ac.knue.commonfoundation.lectureevaluations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIdentifierFilter;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 강의평가 실적의 검색과 저장 HTTP 계약을 제공한다.
 * 인증된 사용자의 역할을 서비스에 전달하여 서버 측 권한 검증을 유지한다.
 */
@RestController
public class LectureEvaluationAchievementController {
    private final LectureEvaluationAchievementService service;

    public LectureEvaluationAchievementController(LectureEvaluationAchievementService service) {
        this.service = service;
    }

    /** 강의평가 실적을 선택 조건과 허용된 페이지 크기로 조회한다. */
    @GetMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementSearchResponse> listLectureEvaluationAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredDateTo,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.list(new LectureEvaluationAchievementSearchCriteria(managementNo, teacherName,
                managementItemCode, occurredDateFrom, occurredDateTo, certificationStatus), page, pageSize, currentUser(servletRequest)),
                RequestIdentifierFilter.requestId(servletRequest));
    }

    /** 강의평가 실적 원천, 상태 이력, 변경 이력을 하나의 트랜잭션으로 저장한다. */
    @PostMapping("/api/business/lecture-evaluation-achievements")
    public ApiResponse<LectureEvaluationAchievementRow> saveLectureEvaluationAchievement(
            @Valid @RequestBody LectureEvaluationAchievementSaveRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(service.save(request, currentUser(servletRequest)), RequestIdentifierFilter.requestId(servletRequest));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) return currentUser;
        throw new UnauthenticatedException();
    }
}
