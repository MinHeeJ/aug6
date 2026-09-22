package kr.ac.knue.commonfoundation.common.api;

import java.util.Comparator;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.schoolinfo.ExternalIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts API exceptions into the shared error envelope without exposing internal failures.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ValidationError> fields = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparingInt(GlobalExceptionHandler::validationFieldPriority)
                        .thenComparing(FieldError::getField))
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiError.validation(fields), request.getHeader("X-Request-Id")));
    }

    private static int validationFieldPriority(FieldError error) {
        return "userId".equals(error.getField()) ? 0 : 1;
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessValidation(
            BusinessValidationException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                new ApiError("VALIDATION_ERROR", exception.getMessage(), exception.fields()),
                request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestPart(
            MissingServletRequestPartException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiError.of("BAD_REQUEST", exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthenticated(
            UnauthenticatedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ApiError.of("UNAUTHENTICATED", exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    /** Retains the caller correlation ID when authorization stops a request before persistence. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ApiError.of("FORBIDDEN", exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ApiError.of("NOT_FOUND", exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ApiError.of(conflictCode(exception), exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    /**
     * Exposes only the explicitly supported business lock codes; all other conflicts retain
     * the shared generic contract rather than reflecting arbitrary exception message text.
     */
    private String conflictCode(ConflictException exception) {
        String message = exception.getMessage();
        if (message != null && (message.startsWith("CONFIRMED_RULE_LOCKED:")
                || message.startsWith("CONFIRMED_DATA_LOCKED:"))) {
            return message.substring(0, message.indexOf(':'));
        }
        return "CONFLICT";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiError.of("BAD_REQUEST", exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(ExternalIntegrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalIntegration(
            ExternalIntegrationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(
                        ApiError.of("EXTERNAL_INTEGRATION_ERROR", exception.getMessage()), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingRoute(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ApiError.of("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ApiError.of("METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(ApiError.of("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."), request.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedError(Exception exception, HttpServletRequest request) {
        log.error("Unexpected system error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(ApiError.of(
                "INTERNAL_ERROR", "오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요."), request.getHeader("X-Request-Id")));
    }

    /**
     * Preserves the direct handler invocation contract used by existing verification tests.
     * Dispatcher-invoked requests use the overload above so the request ID is retained.
     */
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedError(Exception exception) {
        log.error("Unexpected system error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(ApiError.of(
                "INTERNAL_ERROR", "오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요.")));
    }
}
