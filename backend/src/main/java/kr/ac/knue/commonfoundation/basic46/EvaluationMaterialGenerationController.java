package kr.ac.knue.commonfoundation.basic46;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationMaterialGenerationController {
    private final EvaluationMaterialGenerationService service;

    public EvaluationMaterialGenerationController(EvaluationMaterialGenerationService service) {
        this.service = service;
    }

    @GetMapping("/api/business/evaluation-material-generations")
    public ApiResponse<EvaluationMaterialGenerationSearchResponse> listEvaluationMaterialGenerations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String organizationCode,
            @RequestParam(required = false) Long targetUserId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.list(new EvaluationMaterialGenerationSearchCriteria(
                page, size, evaluationYear, areaCode, organizationCode, targetUserId)));
    }

    @PostMapping("/api/business/evaluation-material-generations")
    public ApiResponse<EvaluationMaterialGenerationResult> createEvaluationMaterialGeneration(
            @Valid @RequestBody EvaluationMaterialGenerationRequest request,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        EvaluationMaterialGenerationResult result = service.create(request, user.userId());
        return ApiResponse.ok(result, result.requestId());
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
