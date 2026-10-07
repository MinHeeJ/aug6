package kr.ac.knue.commonfoundation.common.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles only opt-in typed education conflicts; all legacy exception handling remains unchanged. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EducationAchievementConflictAdvice {
    /** Returns exact business error codes without losing the command's audit correlation identifier. */
    @ExceptionHandler(EducationAchievementConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(EducationAchievementConflictException exception) {
        ApiResponse<Void> response = ApiResponse.fail(ApiError.of(exception.code(), exception.getMessage()));
        response.meta().put("requestId", exception.requestId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
