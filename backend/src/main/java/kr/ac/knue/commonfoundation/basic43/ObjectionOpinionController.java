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
public class ObjectionOpinionController {
    private final ObjectionOpinionService service;

    public ObjectionOpinionController(ObjectionOpinionService service) {
        this.service = service;
    }

    @GetMapping("/api/business/objection-opinions")
    public ApiResponse<ObjectionOpinionSearchResponse> listObjectionOpinions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String decisionResult,
            @RequestParam(required = false) String applicantName,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.list(new ObjectionOpinionSearchCriteria(
                page, size, evaluationYear, decisionResult, applicantName)));
    }

    @PostMapping("/api/business/objection-opinions/{targetId}/transition")
    public ApiResponse<ObjectionOpinionRow> saveObjectionOpinionsTransition(
            @PathVariable String targetId,
            @Valid @RequestBody ObjectionOpinionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        Long objectionId;
        try {
            objectionId = Long.valueOf(targetId);
        } catch (NumberFormatException exception) {
            objectionId = -1L;
        }
        validateTransitionRequest(request);
        return ApiResponse.ok(service.transition(objectionId, request, user.userId()));
    }

    private void validateTransitionRequest(ObjectionOpinionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String decisionResult = request == null || request.decisionResult() == null ? "" : request.decisionResult().trim().toUpperCase();
        if (!List.of("ACCEPTED", "REJECTED", "NEEDS_REVIEW").contains(decisionResult)) {
            fields.add(new ValidationError("decisionResult", "ACCEPTED, REJECTED, NEEDS_REVIEW 중 하나를 선택하세요."));
        }
        if (request == null || request.reviewerOpinion() == null || request.reviewerOpinion().isBlank()) {
            fields.add(new ValidationError("reviewerOpinion", "검토자 의견을 입력하세요."));
        }
        if ("REJECTED".equals(decisionResult) && (request.reasonCode() == null || request.reasonCode().isBlank())) {
            fields.add(new ValidationError("reasonCode", "기각 사유를 선택하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("이의신청 의견 처리 요청이 올바르지 않습니다.", fields);
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
