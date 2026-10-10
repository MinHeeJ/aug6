package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/** Feature-local error codes and correlation IDs; unrelated global error handling is preserved. */
@RestControllerAdvice(assignableTypes = EmploymentRateImprovementController.class)
@Order(-100)
public class EmploymentRateImprovementExceptionHandler {
    @ExceptionHandler(EmploymentRateImprovementConflict.class)
    public ResponseEntity<ApiResponse<Void>> conflict(
            EmploymentRateImprovementConflict error, HttpServletRequest request) {
        return failure(409, error.code(), "실적의 기간 또는 상태를 확인하세요.", List.of(), request);
    }

    @ExceptionHandler({BusinessValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> validation(Exception error, HttpServletRequest request) {
        List<ValidationError> fields = error instanceof BusinessValidationException validation
                ? validation.fields()
                : ((MethodArgumentNotValidException) error).getBindingResult().getFieldErrors().stream()
                    .map(field -> new ValidationError(field.getField(), field.getDefaultMessage())).toList();
        return failure(400, "VALIDATION_ERROR", "입력값을 확인하세요.", fields, request);
    }

    @ExceptionHandler({ForbiddenException.class, UnauthenticatedException.class, NotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> access(RuntimeException error, HttpServletRequest request) {
        int status = error instanceof ForbiddenException ? 403 : error instanceof UnauthenticatedException ? 401 : 404;
        String code = status == 403 ? "FORBIDDEN" : status == 401 ? "UNAUTHENTICATED" : "NOT_FOUND";
        return failure(status, code, error.getMessage(), List.of(), request);
    }

    private ResponseEntity<ApiResponse<Void>> failure(
            int status, String code, String message, List<ValidationError> fields, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, null, new ApiError(code, message, fields),
                Map.of("requestId", String.valueOf(request.getAttribute("requestId")))));
    }
}
