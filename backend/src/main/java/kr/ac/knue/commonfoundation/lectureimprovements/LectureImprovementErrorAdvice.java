package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Keeps malformed boundary input at 400 without leaking parser internals or altering legacy errors. */
@Order(-1)
@RestControllerAdvice(assignableTypes = LectureImprovementController.class)
public class LectureImprovementErrorAdvice {
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> malformed(Exception exception, HttpServletRequest request) {
        return badRequest(new ApiError("VALIDATION_ERROR", "입력 형식과 날짜·숫자 값을 확인하세요.", List.of()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> fields(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ValidationError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage())).toList();
        return badRequest(ApiError.validation(fields), request);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessValidationException exception,
            HttpServletRequest request) {
        return badRequest(new ApiError("VALIDATION_ERROR", exception.getMessage(), exception.fields()), request);
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(ApiError error, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, error,
                Map.of("requestId", RequestIds.resolve(request))));
    }
}
