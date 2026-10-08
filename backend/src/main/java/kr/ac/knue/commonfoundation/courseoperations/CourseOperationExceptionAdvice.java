package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Scoped object-field errors preserve the legacy global list-field response contract. */
@RestControllerAdvice(assignableTypes = CourseOperationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CourseOperationExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception failure, HttpServletRequest request) {
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = "처리 중 오류가 발생했습니다. 관리자에게 문의하세요.";
        Map<String, String> fields = new LinkedHashMap<>();
        if (failure instanceof MethodArgumentNotValidException invalid) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "입력값을 확인하세요.";
            invalid.getBindingResult().getFieldErrors().forEach(error ->
                    fields.put(error.getField(), error.getDefaultMessage()));
        } else if (failure instanceof BusinessValidationException invalid) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = invalid.getMessage();
            invalid.fields().forEach(error -> fields.put(error.field(), error.message()));
        } else if (failure instanceof HttpMessageNotReadableException
                || failure instanceof MethodArgumentTypeMismatchException) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "요청 형식이 올바르지 않습니다.";
        } else if (failure instanceof UnauthenticatedException) {
            status = 401;
            code = "UNAUTHENTICATED";
            message = "인증이 필요합니다.";
        } else if (failure instanceof ForbiddenException) {
            status = 403;
            code = "FORBIDDEN";
            message = "접근 권한이 없습니다.";
        } else if (failure instanceof NotFoundException) {
            status = 404;
            code = "NOT_FOUND";
            message = "실적을 찾을 수 없습니다.";
        } else if (failure instanceof ConflictException) {
            status = 409;
            String prefix = failure.getMessage().split(":", 2)[0];
            code = switch (prefix) {
                case "CONFIRMED_DATA_LOCKED", "PERIOD_NOT_ACTIVE", "INVALID_STATE_TRANSITION" -> prefix;
                default -> "CONFLICT";
            };
            message = failure.getMessage();
        } else if (failure instanceof DataIntegrityViolationException) {
            status = 409;
            code = "CONFLICT";
            message = "중복 실적 또는 저장 제약 충돌입니다.";
        }
        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "error", Map.of("code", code, "message", message, "fields", fields),
                "meta", Map.of("requestId", CourseOperationController.trace(request))));
    }
}
