package kr.ac.knue.commonfoundation.basic45;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScoreRecalculationController {
    private final ScoreRecalculationService service;

    public ScoreRecalculationController(ScoreRecalculationService service) {
        this.service = service;
    }

    @GetMapping("/api/business/score-recalculations/preview")
    public ApiResponse<ScoreRecalculationPreviewResponse> listScoreRecalculationTargets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String targetUserId,
            @RequestParam(required = false) String formulaVersionId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.preview(new ScoreRecalculationSearchCriteria(
                page, size, evaluationYear, areaCode, targetUserId, formulaVersionId)));
    }

    @PostMapping("/api/business/score-recalculations")
    public ApiResponse<ScoreRecalculationResult> createScoreRecalculation(
            @RequestBody(required = false) EvaluationBatchActionRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        validateCreateRequest(request);
        ScoreRecalculationResult result = service.recalculate(request, user.userId());
        return ApiResponse.ok(result, result.requestId());
    }

    private void validateCreateRequest(EvaluationBatchActionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        }
        if (request == null || request.formulaVersionId() == null || request.formulaVersionId().isBlank()) {
            fields.add(new ValidationError("formulaVersionId", "산식버전을 선택하세요."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("점수 재계산 요청이 올바르지 않습니다.", fields);
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
