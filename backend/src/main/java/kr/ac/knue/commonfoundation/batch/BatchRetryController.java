package kr.ac.knue.commonfoundation.batch;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchRetryController {
    private final BatchRetryService service;

    public BatchRetryController(BatchRetryService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/batch-retries/targets")
    public ApiResponse<BatchRetryTargetSearchResponse> listBatchRetryTargets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String originalExecutionId,
            @RequestParam(required = false) String failedItemKey) {
        return ApiResponse.ok(service.listBatchRetryTargets(page, size,
                new BatchRetryTargetSearchCriteria(originalExecutionId, failedItemKey)));
    }

    @PostMapping("/api/admin/batch-retries")
    public ApiResponse<BatchRetryResultRow> createBatchRetry(@Valid @RequestBody BatchRetryRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        BatchRetryResultRow result = service.createBatchRetry(request, currentUser(servletRequest).userId(), requestId);
        return ApiResponse.ok(result, result.requestId());
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
