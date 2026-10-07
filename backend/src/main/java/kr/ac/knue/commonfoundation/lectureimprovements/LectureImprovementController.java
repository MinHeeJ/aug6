package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

/** The four approved FR-031 endpoints, with session roles and correlated success/error envelopes. */
@RestController
@RequestMapping("/api/business/lecture-improvements")
public class LectureImprovementController {
    private final LectureImprovementService service;

    public LectureImprovementController(LectureImprovementService service) {
        this.service = service;
    }

    /** Returns a paginated, caller-scoped list with validated paging and dynamic optional filters. */
    @GetMapping
    public ApiResponse<LectureImprovementSearchResponse> listLectureImprovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus,
            @RequestParam(required = false) String certificationStatus,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            HttpServletRequest request) {
        CurrentUser user = current(request, false);
        if (page < 0 || !List.of(20, 50, 100).contains(pageSize)) {
            throw new BusinessValidationException("페이지 조건을 확인하세요.",
                    List.of(new ValidationError("pageSize", "페이지는 0 이상, 표시 건수는 20/50/100이어야 합니다.")));
        }
        LectureImprovementSearchCriteria criteria = new LectureImprovementSearchCriteria(
                page, pageSize, (long) page * pageSize, normalized(managementNo), normalized(teacherName),
                normalized(managementItemCode), normalized(achievementStatus == null
                        ? certificationStatus : achievementStatus), normalized(academicYear), normalized(semester));
        return ApiResponse.ok(service.list(criteria, user), requestId(request));
    }

    /** Loads the selected detail through the same role/scope boundary as its list. */
    @GetMapping("/{achievementId}")
    public ApiResponse<LectureImprovementRow> getLectureImprovement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, current(request, false)), requestId(request));
    }

    /** Creates a new DRAFT record owned by the authenticated R01 principal. */
    @PostMapping
    public ApiResponse<LectureImprovementSaveResult> createLectureImprovement(
            @Valid @RequestBody LectureImprovementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, current(request, true), requestId(request)), requestId(request));
    }

    /** Updates only the path-selected source, never an identity or status supplied by the client. */
    @PutMapping("/{achievementId}")
    public ApiResponse<LectureImprovementSaveResult> updateLectureImprovement(
            @PathVariable Long achievementId,
            @Valid @RequestBody LectureImprovementRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.update(achievementId, body, current(request, true), requestId(request)),
                requestId(request));
    }

    /** Keeps existing error codes/fields and traceId while adding the same requestId used in persistence. */
    @ExceptionHandler({BusinessValidationException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, ForbiddenException.class, UnauthenticatedException.class,
            NotFoundException.class, ConflictException.class})
    public ResponseEntity<ApiResponse<Void>> error(Exception exception, HttpServletRequest request) {
        int status = 400;
        ApiError error;
        if (exception instanceof BusinessValidationException validation) {
            error = new ApiError("VALIDATION_ERROR", validation.getMessage(), validation.fields());
        } else if (exception instanceof MethodArgumentNotValidException validation) {
            error = ApiError.validation(validation.getBindingResult().getFieldErrors().stream()
                    .flatMap(field -> {
                        ValidationError canonical = new ValidationError(field.getField(), field.getDefaultMessage());
                        // The completed fixture calls this field achievementContent; D5 uses performanceContent.
                        return "performanceContent".equals(field.getField())
                                ? java.util.stream.Stream.of(canonical,
                                        new ValidationError("achievementContent", field.getDefaultMessage()))
                                : java.util.stream.Stream.of(canonical);
                    }).toList());
        } else if (exception instanceof ForbiddenException) {
            status = 403;
            error = ApiError.of("FORBIDDEN", exception.getMessage());
        } else if (exception instanceof UnauthenticatedException) {
            status = 401;
            error = ApiError.of("UNAUTHENTICATED", exception.getMessage());
        } else if (exception instanceof NotFoundException) {
            status = 404;
            error = ApiError.of("NOT_FOUND", exception.getMessage());
        } else if (exception instanceof ConflictException) {
            status = 409;
            error = ApiError.of("CONFLICT", exception.getMessage());
        } else {
            error = ApiError.of("VALIDATION_ERROR", "입력값 형식을 확인하세요.");
        }
        ApiResponse<Void> envelope = ApiResponse.fail(error);
        envelope.meta().put("requestId", requestId(request));
        return ResponseEntity.status(status).body(envelope);
    }

    private CurrentUser current(HttpServletRequest request, boolean write) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser value ? value : null;
        LectureImprovementService.requireRole(user, write);
        return user;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute("lectureImprovementRequestId");
        if (existing != null) return existing.toString();
        String supplied = request.getHeader("X-Request-Id");
        String id = supplied != null && supplied.matches("[A-Za-z0-9._:-]{1,100}")
                ? supplied : UUID.randomUUID().toString();
        request.setAttribute("lectureImprovementRequestId", id);
        return id;
    }
}
