package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Approved FR-029 HTTP entrypoints; error envelopes retain the shared model and this request's correlation ID. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Returns the scoped paged list; optional filters are normalized before SQL binding. */
    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new EmploymentRateImprovementCriteria(
                page, pageSize, 0, managementNo, teacherName, managementItemCode, achievementStatus), user(request)),
                requestId(request));
    }

    /** Reads a typed detail only inside the same role/scope union used by the list. */
    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId,
            HttpServletRequest request) {
        return ApiResponse.ok(service.detail(achievementId, user(request)), requestId(request));
    }

    /** Creates an owned DRAFT through the real atomic service. */
    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, user(request), requestId(request)), requestId(request));
    }

    /** Updates only an owned editable row; the path ID is the authoritative identity. */
    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(
                service.update(achievementId, body, user(request), requestId(request)), requestId(request));
    }

    private CurrentUser user(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser current)) {
            throw new UnauthenticatedException();
        }
        // The service repeats the operation-specific role and ownership checks, including direct callers.
        return current;
    }

    private String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute("employmentImprovementRequestId");
        if (existing instanceof String value) {
            return value;
        }
        String supplied = request.getHeader("X-Request-Id");
        String id = supplied == null || supplied.isBlank() || supplied.length() > 100
                ? UUID.randomUUID().toString() : supplied.trim();
        request.setAttribute("employmentImprovementRequestId", id);
        return id;
    }

    /** Uses the shared status/error translation without changing envelopes of unrelated controllers. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleFailure(Exception exception, HttpServletRequest request) {
        var shared = new GlobalExceptionHandler();
        ResponseEntity<ApiResponse<Void>> response;
        if (exception instanceof MethodArgumentNotValidException validation) {
            response = shared.handleValidation(validation);
        } else if (exception instanceof BusinessValidationException validation) {
            response = shared.handleBusinessValidation(validation);
        } else if (exception instanceof UnauthenticatedException unauthenticated) {
            response = shared.handleUnauthenticated(unauthenticated);
        } else if (exception instanceof ForbiddenException forbidden) {
            response = shared.handleForbidden(forbidden);
        } else if (exception instanceof NotFoundException missing) {
            response = shared.handleNotFound(missing);
        } else if (exception instanceof ConflictException conflict) {
            response = shared.handleConflict(conflict);
        } else if (exception instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
            response = shared.handleBadRequest(new IllegalArgumentException("요청 JSON 또는 날짜 형식을 확인하세요."));
        } else if (exception instanceof IllegalArgumentException invalid) {
            response = shared.handleBadRequest(invalid);
        } else {
            response = shared.handleUnexpectedError(exception);
        }
        var body = response.getBody();
        var meta = new LinkedHashMap<>(body.meta());
        meta.put("requestId", requestId(request));
        return ResponseEntity.status(response.getStatusCode())
                .body(new ApiResponse<>(false, null, body.error(), meta));
    }
}
