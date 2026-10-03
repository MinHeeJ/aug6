package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the approved 취업률 제고 list, detail, create, and update HTTP contract. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Lists caller-scoped 취업률 제고 rows using the approved page-size contract. */
    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireReadRole(request);
        return ApiResponse.ok(
                service.list(
                        new EmploymentRateImprovementSearchCriteria(
                                page,
                                pageSize,
                                managementNo,
                                teacherName,
                                managementItemCode,
                                certificationStatus),
                        user),
                effectiveRequestId(requestId));
    }

    /** Reads a single row only after the service verifies the caller's data scope. */
    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.get(achievementId, requireReadRole(request)),
                effectiveRequestId(requestId));
    }

    /** Creates an R01-owned row through the guarded atomic service transaction. */
    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> create(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.create(body, requireWriteRole(request), traceId), traceId);
    }

    /** Updates an R01-owned unlocked row through the guarded atomic service transaction. */
    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = effectiveRequestId(requestId);
        return ApiResponse.ok(service.update(achievementId, body, requireWriteRole(request), traceId), traceId);
    }

    private CurrentUser requireReadRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || user.roles().stream().noneMatch(List.of("R01", "R02", "R04")::contains)) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriteRole(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
    }

    private String effectiveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
