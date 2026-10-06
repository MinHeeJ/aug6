package kr.ac.knue.commonfoundation.courseoperations;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Feature-local contract errors; malformed input never exposes parser, SQL or Java internals. */
@RestControllerAdvice(assignableTypes = CourseOperationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CourseOperationExceptionHandler {
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> invalidBody(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ApiError.validation(
                List.of(new ValidationError("body", "허용된 입력 항목과 날짜·식별자 형식을 확인하세요.")))));
    }

    /** Promotes the shared guard's semantic lock/period prefix into the approved error code. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(ConflictException exception) {
        String message = exception.getMessage();
        String code = "CONFLICT";
        for (String candidate : List.of("PERIOD_NOT_ACTIVE", "CONFIRMED_DATA_LOCKED", "INVALID_STATE_TRANSITION")) {
            if (message != null && message.startsWith(candidate + ":")) {
                code = candidate;
                break;
            }
        }
        return ResponseEntity.status(409).body(ApiResponse.fail(ApiError.of(code, message)));
    }
}
