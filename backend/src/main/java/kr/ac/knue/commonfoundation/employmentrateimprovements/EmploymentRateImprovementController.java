package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Approved FR-029 HTTP entrypoints, with separate create/update and existing response envelopes. */
@RestController
@RequestMapping("/api/business/employment-rate-improvements")
public class EmploymentRateImprovementController {
    private final EmploymentRateImprovementService service;

    public EmploymentRateImprovementController(EmploymentRateImprovementService service) {
        this.service = service;
    }

    /** Returns scoped, filtered rows and an identically filtered total. */
    @GetMapping
    public ApiResponse<EmploymentRateImprovementSearchResponse> listEmploymentRateImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String certificationStatus,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(new EmploymentRateImprovementSearchCriteria(
                page, pageSize, 0, managementNo, teacherName, managementItemCode, certificationStatus),
                principal(request)), trace(request));
    }

    /** Reads one existing achievement after the service rechecks ownership/data scope. */
    @GetMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementRow> getEmploymentRateImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, principal(request)), trace(request));
    }

    /** Creates only for the authenticated R01 teacher, never for a body-supplied identity. */
    @PostMapping
    public ApiResponse<EmploymentRateImprovementSaveResult> createEmploymentRateImprovement(
            @Valid @RequestBody EmploymentRateImprovementRequest body, HttpServletRequest request) {
        String requestId = trace(request);
        return ApiResponse.ok(service.create(body, principal(request), requestId), requestId);
    }

    /** Updates only the selected path identity; absent rows remain 404, not an implicit insert. */
    @PutMapping("/{achievementId}")
    public ApiResponse<EmploymentRateImprovementSaveResult> updateEmploymentRateImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateImprovementRequest body,
            HttpServletRequest request) {
        String requestId = trace(request);
        return ApiResponse.ok(service.update(achievementId, body, principal(request), requestId), requestId);
    }

    /** Prevents Jackson/parser internals and forged field values from leaking in a validation response. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformedBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                ApiError.of("BAD_REQUEST", "요청 필드 또는 날짜 형식이 올바르지 않습니다.")));
    }

    /** Keeps the shared envelope while surfacing the approved period/finalization conflict identifier. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(ConflictException exception) {
        String message = exception.getMessage();
        String code = message != null && message.contains(":") ? message.split(":", 2)[0] : "CONFLICT";
        return ResponseEntity.status(409).body(ApiResponse.fail(ApiError.of(code, message)));
    }

    private CurrentUser principal(HttpServletRequest request) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        return user;
    }

    private String trace(HttpServletRequest request) {
        String value = request.getHeader("X-Request-Id");
        return value != null && value.matches("[A-Za-z0-9._:-]{1,100}")
                ? value : UUID.randomUUID().toString();
    }
}
