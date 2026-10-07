package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Keeps malformed course input a safe, correlated 400 without changing legacy error handling. */
@RestControllerAdvice(assignableTypes = CourseOperationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CourseOperationInputAdvice {
    /** Bean validation preserves the shared list-shaped field error contract. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ValidationError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new ValidationError(field.getField(), field.getDefaultMessage()))
                .toList();
        return response(ApiError.validation(fields), request);
    }

    /** Parser internals, Java type names and storage details never enter a user-facing response. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> unreadable(Exception exception, HttpServletRequest request) {
        return response(ApiError.of("BAD_REQUEST", "입력 형식과 날짜·식별자를 확인하세요."), request);
    }

    private ResponseEntity<ApiResponse<Void>> response(ApiError error, HttpServletRequest request) {
        ApiResponse<Void> body = ApiResponse.fail(error);
        body.meta().put("requestId", RequestIds.resolve(request));
        return ResponseEntity.badRequest().body(body);
    }
}
