package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Feature-local typed errors and trace metadata without changing legacy error envelopes. */
@Order(-10)
@RestControllerAdvice(assignableTypes = CourseOperationController.class)
public class CourseOperationExceptionHandler {
    @ModelAttribute
    public void initializeRequestId(HttpServletRequest request) {
        requestId(request);
    }

    public static String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute("courseOperationRequestId");
        if (existing != null) return existing.toString();
        String header = request.getHeader("X-Request-Id");
        String id = header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
        request.setAttribute("courseOperationRequestId", id);
        return id;
    }

    @ExceptionHandler(CourseOperationConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(CourseOperationConflictException exception,
            HttpServletRequest request) {
        return error(409, ApiError.of(exception.code(), exception.getMessage()), request);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Void>> businessValidation(BusinessValidationException exception,
            HttpServletRequest request) {
        return error(400, new ApiError("VALIDATION_ERROR", exception.getMessage(), exception.fields()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ValidationError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new ValidationError(field.getField(), field.getDefaultMessage())).toList();
        return error(400, ApiError.validation(fields), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> malformed(Exception exception, HttpServletRequest request) {
        return error(400, ApiError.of("VALIDATION_ERROR", "요청 형식이 올바르지 않습니다."), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(ForbiddenException exception, HttpServletRequest request) {
        return error(403, ApiError.of("FORBIDDEN", "접근 권한이 없습니다."), request);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Void>> unauthenticated(UnauthenticatedException exception,
            HttpServletRequest request) {
        return error(401, ApiError.of("UNAUTHENTICATED", "인증이 필요합니다."), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> missing(NotFoundException exception, HttpServletRequest request) {
        return error(404, ApiError.of("NOT_FOUND", "실적을 찾을 수 없습니다."), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception exception, HttpServletRequest request) {
        return error(500, ApiError.of("INTERNAL_ERROR", "처리 중 오류가 발생했습니다. 다시 시도하세요."), request);
    }

    private ResponseEntity<ApiResponse<Void>> error(int status, ApiError error, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(error);
        response.meta().put("requestId", requestId(request));
        return ResponseEntity.status(status).body(response);
    }
}
