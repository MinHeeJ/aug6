package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/** Keeps feature diagnostics and the incoming correlation ID in non-leaking error envelopes. */
@RestControllerAdvice(assignableTypes = EmploymentRateAchievementController.class)
@Order(-10)
public class EmploymentRateErrorAdvice {
    @ExceptionHandler(EmploymentRateException.class)
    public ResponseEntity<ApiResponse<Object>> conflict(EmploymentRateException error, HttpServletRequest request) {
        var meta = new LinkedHashMap<String, Object>(error.details);
        meta.put("requestId", EmploymentRateAchievementController.trace(request));
        return ResponseEntity.status(error.status).body(new ApiResponse<>(false, null,
                ApiError.of(error.code, error.getMessage()), meta));
    }

    @ExceptionHandler({ForbiddenException.class, UnauthenticatedException.class, NotFoundException.class,
            BusinessValidationException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Object>> failure(Exception error, HttpServletRequest request) {
        int status = 400;
        String code = "VALIDATION_ERROR";
        String message = "입력값을 확인해 주세요.";
        List<ValidationError> fields = List.of();
        if (error instanceof ForbiddenException) {
            status = 403; code = "FORBIDDEN"; message = "접근 권한이 없습니다.";
        } else if (error instanceof UnauthenticatedException) {
            status = 401; code = "UNAUTHENTICATED"; message = "인증이 필요합니다.";
        } else if (error instanceof NotFoundException) {
            status = 404; code = "NOT_FOUND"; message = "대상을 찾을 수 없습니다.";
        } else if (error instanceof MethodArgumentNotValidException invalid) {
            fields = invalid.getBindingResult().getFieldErrors().stream()
                    .map(f -> new ValidationError(f.getField(), f.getDefaultMessage())).toList();
        } else if (error instanceof BusinessValidationException invalid) {
            fields = invalid.fields();
        }
        return ResponseEntity.status(status).body(new ApiResponse<>(false, null,
                new ApiError(code, message, fields),
                java.util.Map.of("requestId", EmploymentRateAchievementController.trace(request))));
    }
}
