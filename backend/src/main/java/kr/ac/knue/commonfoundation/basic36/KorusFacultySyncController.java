package kr.ac.knue.commonfoundation.basic36;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KorusFacultySyncController {
    private final KorusFacultySyncService service;

    public KorusFacultySyncController(KorusFacultySyncService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/korus-faculty-sync-results")
    public ApiResponse<KorusFacultySyncSearchResponse> listKorusFacultySyncResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LocalDate targetStartDate,
            @RequestParam(required = false) LocalDate targetEndDate,
            @RequestParam(required = false) String syncStatus,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String employeeNo,
            @RequestHeader(value = "X-Request-Id", required = false) String headerRequestId,
            HttpServletRequest servletRequest) {
        requireSyncRole(servletRequest);
        validatePageSize(size);
        String effectiveRequestId = effectiveRequestId(headerRequestId);
        return ApiResponse.ok(service.list(new KorusFacultySyncSearchCriteria(page, size, targetStartDate,
                targetEndDate, syncStatus, requestId, employeeNo)), effectiveRequestId);
    }

    @PostMapping("/api/admin/korus-faculty-sync-runs")
    public ApiResponse<KorusFacultySyncRunRow> createKorusFacultySyncRun(
            @Valid @RequestBody KorusFacultySyncRunRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSyncRole(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.createRun(request, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    @PostMapping("/api/admin/korus-faculty-sync-results/{syncResultId}/retry")
    public ApiResponse<KorusFacultySyncRunRow> createKorusFacultySyncRetry(
            @PathVariable("syncResultId") Long syncResultId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest servletRequest) {
        CurrentUser user = requireSyncRole(servletRequest);
        String effectiveRequestId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.retryFailedResult(syncResultId, user.userId(), effectiveRequestId), effectiveRequestId);
    }

    private CurrentUser requireSyncRole(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R04") || currentUser.roles().contains("R09")) {
                return currentUser;
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private void validatePageSize(int size) {
        if (size != 20 && size != 50 && size != 100) {
            throw new BusinessValidationException("KORUS 교원 동기화 목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isBlank()) return requestId.trim();
        return UUID.randomUUID().toString();
    }
}
