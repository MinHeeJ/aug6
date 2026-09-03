package kr.ac.knue.commonfoundation.basic45;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationBatchResultController {
    private final EvaluationBatchResultService service;

    public EvaluationBatchResultController(EvaluationBatchResultService service) {
        this.service = service;
    }

    @GetMapping("/api/business/evaluation-batch-results")
    public ApiResponse<EvaluationBatchResultListResponse> listEvaluationBatchResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String targetCondition,
            HttpServletRequest servletRequest) {
        requireAnyRole(servletRequest, List.of("R09"));
        return ApiResponse.ok(service.list(new EvaluationBatchResultSearchCriteria(
                page, size, batchId, jobType, targetCondition)));
    }

    @GetMapping("/api/business/evaluation-batch-results/{batchId}/errors")
    public ApiResponse<EvaluationBatchResultErrorListResponse> listEvaluationBatchResultErrors(
            @PathVariable String batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest) {
        requireAnyRole(servletRequest, List.of("R09"));
        return ApiResponse.ok(service.listErrors(batchId, page, size));
    }

    private CurrentUser requireAnyRole(HttpServletRequest request, List<String> allowedRoles) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().stream().anyMatch(allowedRoles::contains)) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }
}
