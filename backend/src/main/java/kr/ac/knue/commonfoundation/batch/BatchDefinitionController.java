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
public class BatchDefinitionController {
    private final BatchDefinitionService service;

    public BatchDefinitionController(BatchDefinitionService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/batch-definitions")
    public ApiResponse<BatchDefinitionSearchResponse> listBatchDefinitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String batchType,
            @RequestParam(required = false) String scheduleCycle) {
        return ApiResponse.ok(service.listBatchDefinitions(page, size,
                new BatchDefinitionSearchCriteria(batchId, batchType, scheduleCycle)));
    }

    @PostMapping("/api/admin/batch-definitions")
    public ApiResponse<BatchDefinitionRow> saveBatchDefinition(@Valid @RequestBody BatchDefinitionRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        BatchDefinitionRow saved = service.saveBatchDefinition(request, currentUser(servletRequest).userId(), requestId);
        return ApiResponse.ok(saved, saved.requestId());
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new UnauthenticatedException();
    }
}
