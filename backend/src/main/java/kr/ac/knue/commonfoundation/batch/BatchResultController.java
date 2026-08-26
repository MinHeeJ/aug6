package kr.ac.knue.commonfoundation.batch;

import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchResultController {
    private final BatchResultService service;

    public BatchResultController(BatchResultService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/batch-results")
    public ApiResponse<BatchResultSearchResponse> listBatchResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String executionStatus) {
        return ApiResponse.ok(service.listBatchResults(page, size,
                new BatchResultSearchCriteria(executionId, batchId, executionStatus)));
    }

    @GetMapping("/api/admin/batch-results/{executionId}/log")
    public ApiResponse<BatchResultLogResponse> getBatchResultLog(@PathVariable String executionId) {
        return ApiResponse.ok(service.getBatchResultLog(executionId));
    }
}
