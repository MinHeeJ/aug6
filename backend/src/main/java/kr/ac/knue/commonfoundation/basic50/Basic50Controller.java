package kr.ac.knue.commonfoundation.basic50;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Basic50Controller {
    private final Basic50Service service;

    public Basic50Controller(Basic50Service service) {
        this.service = service;
    }

    @GetMapping("/api/business/college-evaluation-unit-authorities")
    public ApiResponse<AuthoritySearchResponse> listCollegeEvaluationUnitAuthorities(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String organizationCode, @RequestParam(required = false) String evaluationUnitCode, @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        validateSize(size);
        return ApiResponse.ok(service.listAuthorities(user(servletRequest), new BusinessSettingCriteria(page, size, evaluationYear, organizationCode, evaluationUnitCode, activeYn, keyword, null, false)), rid(requestId));
    }

    @PostMapping("/api/business/college-evaluation-unit-authorities/save")
    public ApiResponse<AuthorityRow> saveCollegeEvaluationUnitAuthority(@Valid @RequestBody CollegeEvaluationUnitAuthoritySaveRequest request, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveAuthority(request, user(servletRequest), rid), rid);
    }

    @GetMapping("/api/business/appeal-business-settings")
    public ApiResponse<BusinessSettingSearchResponse> listAppealBusinessSettings(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String organizationCode, @RequestParam(required = false) String evaluationUnitCode, @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        validateSize(size);
        return ApiResponse.ok(service.listAppealSettings(user(servletRequest), new BusinessSettingCriteria(page, size, evaluationYear, organizationCode, evaluationUnitCode, activeYn, keyword, null, false)), rid(requestId));
    }

    @PostMapping("/api/business/appeal-business-settings/save")
    public ApiResponse<BusinessSettingRow> saveAppealBusinessSetting(@Valid @RequestBody BusinessSettingSaveRequest request, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveAppealSetting(request, user(servletRequest), rid), rid);
    }

    @GetMapping("/api/business/result-view-business-settings")
    public ApiResponse<BusinessSettingSearchResponse> listResultViewBusinessSettings(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String organizationCode, @RequestParam(required = false) String evaluationUnitCode, @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        validateSize(size);
        return ApiResponse.ok(service.listResultSettings(user(servletRequest), new BusinessSettingCriteria(page, size, evaluationYear, organizationCode, evaluationUnitCode, activeYn, keyword, null, false)), rid(requestId));
    }

    @PostMapping("/api/business/result-view-business-settings/save")
    public ApiResponse<BusinessSettingRow> saveResultViewBusinessSetting(@Valid @RequestBody BusinessSettingSaveRequest request, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveResultSetting(request, user(servletRequest), rid), rid);
    }

    @GetMapping("/api/business/personal-achievement-scores")
    public ApiResponse<PersonalAchievementScoreResponse> getPersonalAchievementScores(@RequestParam(required = false) Long teacherUserId, @RequestParam String evaluationYear, @RequestParam(required = false) String areaCode, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.personalScores(user(servletRequest), teacherUserId, evaluationYear, areaCode, rid), rid);
    }

    @GetMapping("/api/business/research-classification-criteria")
    public ApiResponse<ResearchCriterionSearchResponse> listResearchClassificationCriteria(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String areaCode, @RequestParam(required = false) String managementCriterionCode, @RequestParam(required = false) String activeYn, @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        validateSize(size);
        return ApiResponse.ok(service.listCriteria(user(servletRequest), new ResearchCriterionCriteria(page, size, areaCode, managementCriterionCode, activeYn, keyword)), rid(requestId));
    }

    @PostMapping("/api/business/research-classification-criteria/save")
    public ApiResponse<ResearchCriterionRow> saveResearchClassificationCriterion(@Valid @RequestBody ResearchCriterionSaveRequest request, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.saveCriterion(request, user(servletRequest), rid), rid);
    }

    @GetMapping("/api/business/unconfirmed-research-achievements")
    public ApiResponse<ResearchAchievementSearchResponse> listUnconfirmedResearchAchievements(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String evaluationYear, @RequestParam(required = false) String organizationCode, @RequestParam(required = false) String areaCode, @RequestParam(required = false) String confirmationStatus, @RequestParam(required = false) Long teacherUserId, @RequestParam(required = false) String keyword, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        validateSize(size);
        String rid = rid(requestId);
        return ApiResponse.ok(service.listUnconfirmedAchievements(user(servletRequest), new ResearchAchievementCriteria(page, size, evaluationYear, organizationCode, areaCode, confirmationStatus == null ? "UNCONFIRMED" : confirmationStatus, teacherUserId, keyword), rid), rid);
    }

    @PostMapping("/api/business/unconfirmed-research-achievements/{achievementId}/confirmation")
    public ApiResponse<ResearchAchievementRow> saveResearchAchievementConfirmation(@PathVariable Long achievementId, @Valid @RequestBody ResearchAchievementConfirmationRequest request, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest servletRequest) {
        String rid = rid(requestId);
        return ApiResponse.ok(service.confirmAchievement(achievementId, request, user(servletRequest), rid), rid);
    }

    private CurrentUser user(HttpServletRequest request) { Object user = request.getAttribute("currentUser"); if (user instanceof CurrentUser currentUser) return currentUser; throw new UnauthenticatedException(); }
    private String rid(String requestId) { return requestId != null && !requestId.isBlank() ? requestId.trim() : UUID.randomUUID().toString(); }
    private void validateSize(int size) { if (size != 20 && size != 50 && size != 100) throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", java.util.List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요."))); }
}
