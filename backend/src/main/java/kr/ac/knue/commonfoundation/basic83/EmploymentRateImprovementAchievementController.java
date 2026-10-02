package kr.ac.knue.commonfoundation.basic83;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the BASIC-83 취업률 제고 list, detail, create, and update HTTP contract. */
@RestController
public class EmploymentRateImprovementAchievementController {
    private final EmploymentRateImprovementAchievementService service;

    public EmploymentRateImprovementAchievementController(
            EmploymentRateImprovementAchievementService service) {
        this.service = service;
    }

    /** Lists caller-scoped 취업률 제고 rows with only the approved page sizes. */
    @GetMapping("/api/business/employment-rate-improvements")
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
        String traceId = traceId(requestId);
        return ApiResponse.ok(
                service.list(
                        new EmploymentRateImprovementSearchCriteria(
                                page,
                                pageSize,
                                managementNo,
                                teacherName,
                                managementItemCode,
                                certificationStatus),
                        requireReadUser(request)),
                traceId);
    }

    /** Returns a caller-authorized detail row. */
    @GetMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> get(
            @PathVariable Long achievementId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.get(achievementId, requireReadUser(request)), traceId);
    }

    /** Creates an R01-owned DRAFT row through the guarded service transaction. */
    @PostMapping("/api/business/employment-rate-improvements")
    public ApiResponse<EmploymentRateImprovementSaveResult> create(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.create(body, requireWriteUser(request), traceId), traceId);
    }

    /** Updates an R01-owned row while preserving service-level period and finalization protections. */
    @PutMapping("/api/business/employment-rate-improvements/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        String traceId = traceId(requestId);
        return ApiResponse.ok(
                service.update(achievementId, body, requireWriteUser(request), traceId),
                traceId);
    }

    private CurrentUser requireReadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null
                || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04", "R09").contains(role))) {
            throw new ForbiddenException();
        }
        return user;
    }

    private CurrentUser requireWriteUser(HttpServletRequest request) {
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

    private String traceId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }
}
