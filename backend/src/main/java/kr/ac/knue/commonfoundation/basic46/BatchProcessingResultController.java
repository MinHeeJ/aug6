package kr.ac.knue.commonfoundation.basic46;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchProcessingResultController {
    private final BatchProcessingResultService service;

    public BatchProcessingResultController(BatchProcessingResultService service) {
        this.service = service;
    }

    @GetMapping("/api/business/batch-processing-results")
    public ApiResponse<BatchProcessingResultSearchResponse> listEvaluationBatchProcessingResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String batchType,
            @RequestParam(required = false) String targetCondition,
            @RequestParam(required = false) String batchId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        return ApiResponse.ok(service.list(new BatchProcessingResultSearchCriteria(
                page, size, batchType, targetCondition, batchId)));
    }

    @GetMapping("/api/business/batch-processing-results/{batchId}/errors")
    public ApiResponse<List<BatchProcessingResultErrorRow>> listEvaluationBatchProcessingResultErrors(
            @PathVariable String batchId,
            HttpServletRequest servletRequest) {
        requireR09(servletRequest);
        String normalizedBatchId = URLDecoder.decode(batchId, StandardCharsets.UTF_8).trim();
        if (normalizedBatchId.isBlank()) {
            throw new BusinessValidationException("배치ID를 입력하세요.", List.of(new ValidationError("batchId", "배치ID를 입력하세요.")));
        }
        return ApiResponse.ok(service.listErrors(normalizedBatchId));
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
