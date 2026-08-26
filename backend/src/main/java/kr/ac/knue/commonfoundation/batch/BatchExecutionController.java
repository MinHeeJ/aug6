package kr.ac.knue.commonfoundation.batch;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchExecutionController {
    private final BatchExecutionService service;

    public BatchExecutionController(BatchExecutionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/batch-executions")
    public ApiResponse<BatchExecutionSearchResponse> listBatchExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String executionStatus) {
        return ApiResponse.ok(service.listBatchExecutions(page, size,
                new BatchExecutionSearchCriteria(batchId, executionStatus)));
    }

    @PostMapping("/api/admin/batch-executions")
    public ApiResponse<BatchExecutionRow> createBatchExecution(@Valid @RequestBody BatchExecutionRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        BatchExecutionRow created = service.createBatchExecution(request, user.userId(), requestId);
        return ApiResponse.ok(created, created.requestId());
    }

    @PatchMapping("/api/admin/batch-executions/{executionId}/status")
    public ApiResponse<BatchExecutionRow> updateBatchExecutionStatus(@PathVariable String executionId,
            @Valid @RequestBody BatchExecutionStatusRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        BatchExecutionRow updated = service.updateBatchExecutionStatus(executionId, request, user.userId(), requestId);
        return ApiResponse.ok(updated, updated.requestId());
    }

    @PostMapping("/api/admin/batch-executions/{executionId}/rerun")
    public ApiResponse<BatchExecutionRow> createBatchRerun(@PathVariable String executionId,
            @Valid @RequestBody BatchExecutionRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireR09(servletRequest);
        BatchExecutionRow rerun = service.createBatchRerun(executionId, request, user.userId(), requestId);
        return ApiResponse.ok(rerun, rerun.requestId());
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
