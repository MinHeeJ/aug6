package kr.ac.knue.commonfoundation.basic43;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AchievementVerificationController {
    private final AchievementVerificationService service;

    public AchievementVerificationController(AchievementVerificationService service) {
        this.service = service;
    }

    @GetMapping("/api/business/achievement-verifications")
    public ApiResponse<AchievementVerificationSearchResponse> listAchievementVerificationTargets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String verificationStatus,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.list(new AchievementVerificationSearchCriteria(
                page, size, evaluationYear, areaCode, verificationStatus)));
    }

    @PostMapping("/api/business/achievement-verifications/{targetId}/transition")
    public ApiResponse<AchievementVerificationRow> saveAchievementVerificationTargetsTransition(
            @PathVariable String targetId,
            @Valid @RequestBody BusinessTransitionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        Long achievementId;
        try {
            achievementId = Long.valueOf(targetId);
        } catch (NumberFormatException exception) {
            achievementId = -1L;
        }
        validateTransitionRequest(request);
        return ApiResponse.ok(service.transition(achievementId, request, user.userId()));
    }

    private void validateTransitionRequest(BusinessTransitionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String actionType = request == null || request.actionType() == null ? "" : request.actionType().trim().toUpperCase();
        if (request == null || request.evidenceRef() == null || request.evidenceRef().isBlank()) {
            fields.add(new ValidationError("evidenceRef", "처리 근거를 입력하세요."));
        }
        if ("RETURN".equals(actionType)) {
            if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                fields.add(new ValidationError("reasonCode", "인증반려 사유를 선택하세요."));
            }
            if (request.opinion() == null || request.opinion().isBlank()) {
                fields.add(new ValidationError("opinion", "인증반려 의견을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("담당자 인증 처리 요청이 올바르지 않습니다.", fields);
        }
    }

    private CurrentUser requireR09(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }
}
